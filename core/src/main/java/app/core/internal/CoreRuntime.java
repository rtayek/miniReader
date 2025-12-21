package app.core.internal;

import app.core.AnswerDto;
import app.core.DocumentDto;
import app.core.MiniReaderConfig;
import app.core.MiniReaderException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class CoreRuntime implements AutoCloseable {
  CoreRuntime(MiniReaderConfig config) throws IOException {
    this.config = Objects.requireNonNull(config, "config");
    this.fetcher = new Fetcher(config);
    this.extractor = new Extractor();
    this.chunker = new Chunker(config);
    this.store = new DocumentStore(config);
    this.index = new LuceneIndex(config);
    this.answerService = new AnswerService(index);
  }

  public IngestResult ingestUrl(String url) throws MiniReaderException {
    try {
      FetchResult fetch = fetch(url);
      String validation = validateFetch(url, fetch);
      if (validation != null) return new IngestResult(null, validation);

      DocumentDto doc = extract(fetch);

      String shellMsg = detectShell(doc);
      if (shellMsg != null) return new IngestResult(doc, shellMsg);

      List<ChunkDto> chunks = chunk(doc);
      persist(doc);
      indexChunks(doc, chunks);

      return new IngestResult(doc, "Saved + indexed (" + chunks.size() + " chunks).");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MiniReaderException("Failed to ingest URL: " + url, e);
    } catch (IOException e) {
      throw new MiniReaderException("Failed to ingest URL: " + url, e);
    }
  }

  public List<Path> listSavedDocs() throws MiniReaderException {
    try {
      return store.list();
    } catch (IOException e) {
      throw new MiniReaderException("Failed to list saved docs", e);
    }
  }

  public DocumentDto loadSavedDoc(Path file) throws MiniReaderException {
    try {
      return store.load(file);
    } catch (IOException e) {
      throw new MiniReaderException("Failed to load saved doc: " + file, e);
    }
  }

  public AnswerDto ask(String question) throws MiniReaderException {
    try {
      return answerService.answer(question);
    } catch (IOException | org.apache.lucene.queryparser.classic.ParseException e) {
      throw new MiniReaderException("Failed to answer question", e);
    }
  }

  public boolean looksLikeJsShell(String plainText) {
    String t = plainText == null ? "" : plainText.strip();
    if (t.length() >= 250) return false;
    String lower = t.toLowerCase();
    return lower.contains("enable javascript")
        || lower.contains("loading")
        || lower.isBlank();
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

  private String validateFetch(String url, FetchResult fetch) {
    if (fetch.statusCode() < 200 || fetch.statusCode() >= 300) {
      return "HTTP " + fetch.statusCode() + " for " + url;
    }
    if (!fetch.contentType().toLowerCase().contains("text/html") && !fetch.contentType().isBlank()) {
      return "Unsupported content-type: " + fetch.contentType();
    }
    return null;
  }

  private DocumentDto extract(FetchResult fetch) {
    return extractor.extract(fetch);
  }

  private String detectShell(DocumentDto doc) throws IOException {
    if (looksLikeJsShell(doc.plainText())) {
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

  public record IngestResult(DocumentDto doc, String message) {}

  private final AnswerService answerService;
  private final Chunker chunker;
  private final DocumentStore store;
  private final Extractor extractor;
  private final Fetcher fetcher;
  private final LuceneIndex index;
  private final MiniReaderConfig config;
}
