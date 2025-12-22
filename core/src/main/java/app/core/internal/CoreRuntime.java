package app.core.internal;

import app.core.AnswerDto;
import app.core.DocumentDto;
import app.core.IngestOutcome;
import app.core.MiniReaderConfig;
import app.core.MiniReaderException;
import app.core.SavedDocDto;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class CoreRuntime implements AutoCloseable {
  CoreRuntime(MiniReaderConfig config) throws IOException {
    this.fetcher = new Fetcher(config);
    this.extractor = new Extractor();
    this.chunker = new Chunker(config);
    this.store = new DocumentStore(config);
    this.index = new LuceneIndex(config);
    this.answerService = new AnswerService(index);
  }

  public IngestOutcome ingestUrl(String url) throws MiniReaderException {
    try {
      FetchResult fetch = fetch(url);
      IngestOutcome validation = validateFetch(fetch);
      if (validation != null) return validation;

      DocumentDto doc = extract(fetch);

      String shellMsg = detectShell(doc);
      if (shellMsg != null) return new IngestOutcome.JsShell(doc, shellMsg);

      List<ChunkDto> chunks = chunk(doc);
      persist(doc);
      indexChunks(doc, chunks);

      return new IngestOutcome.SavedIndexed(doc, chunks.size());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MiniReaderException("Interrupted fetching " + url, e);
    } catch (IOException e) {
      String msg = e.getMessage() == null ? "Fetch failed" : e.getMessage();
      return new IngestOutcome.FetchError(msg, e.getClass().getSimpleName());
    } catch (Exception e) {
      throw new MiniReaderException("Failed to ingest URL: " + url, e);
    }
  }

  public List<SavedDocDto> listSavedDocs() throws MiniReaderException {
    try {
      return store.list();
    } catch (IOException e) {
      throw new MiniReaderException("Failed to list saved docs", e);
    }
  }

  public DocumentDto loadSavedDoc(String id) throws MiniReaderException {
    try {
      return store.load(id);
    } catch (IOException e) {
      throw new MiniReaderException("Failed to load saved doc: " + id, e);
    }
  }

  public AnswerDto ask(String question) throws MiniReaderException {
    try {
      return answerService.answer(question);
    } catch (IOException | org.apache.lucene.queryparser.classic.ParseException e) {
      throw new MiniReaderException("Failed to answer question", e);
    }
  }

  @Override
  public void close() throws MiniReaderException {
    try {
      index.close();
    } catch (IOException e) {
      throw new MiniReaderException("Failed to close core", e);
    }
  }

  private FetchResult fetch(String url) throws IOException, InterruptedException {
    return fetcher.fetch(url);
  }

  private IngestOutcome validateFetch(FetchResult fetch) {
    if (fetch.statusCode() < 200 || fetch.statusCode() >= 300) {
      return new IngestOutcome.HttpError(fetch.statusCode(), snippet(fetch.body()));
    }
    if (!fetch.contentType().toLowerCase().contains("text/html") && !fetch.contentType().isBlank()) {
      return new IngestOutcome.RejectedNonHtml(fetch.contentType());
    }
    return null;
  }

  private DocumentDto extract(FetchResult fetch) {
    return extractor.extract(fetch);
  }

  private String detectShell(DocumentDto doc) throws IOException {
    if (ShellHeuristics.looksLikeJsShell(doc.plainText())) {
      store.save(doc);
      return "This page looks JS-rendered (SPA shell). Extracted text may be empty.";
    }
    return null;
  }

  private List<ChunkDto> chunk(DocumentDto doc) {
    return chunker.chunk(doc);
  }

  private void persist(DocumentDto doc) throws IOException {
    store.save(doc);
  }

  private void indexChunks(DocumentDto doc, List<ChunkDto> chunks) throws IOException {
    index.index(doc, chunks);
  }
  
  private String snippet(String body) {
    if (body == null) return "";
    return body.substring(0, Math.min(body.length(), ERROR_SNIPPET_CHARS));
  }

  private static final int ERROR_SNIPPET_CHARS = 400;

  private final AnswerService answerService;
  private final Chunker chunker;
  private final DocumentStore store;
  private final Extractor extractor;
  private final Fetcher fetcher;
  private final LuceneIndex index;
}
