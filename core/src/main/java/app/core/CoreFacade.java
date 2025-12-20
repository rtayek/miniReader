package app.core;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class CoreFacade implements AutoCloseable {
  public CoreFacade() throws IOException {
    this(MiniReaderConfig.defaults());
  }

  public CoreFacade(MiniReaderConfig config) throws IOException {
    this.config = Objects.requireNonNull(config, "config");
    this.fetcher = new Fetcher();
    this.extractor = new Extractor();
    this.chunker = new Chunker();
    this.store = new DocumentStore(config);
    this.index = new LuceneIndex(config);
    this.answerService = new AnswerService(index);
  }

  public IngestResult ingestUrl(String url) throws IOException, InterruptedException {
    FetchResult fetch = fetcher.fetch(url);

    if (fetch.statusCode() < 200 || fetch.statusCode() >= 300) {
      return new IngestResult(null, "HTTP " + fetch.statusCode() + " for " + url);
    }

    if (!fetch.contentType().toLowerCase().contains("text/html") && !fetch.contentType().isBlank()) {
      return new IngestResult(null, "Unsupported content-type: " + fetch.contentType());
    }

    DocumentDto doc = extractor.extract(fetch);

    if (looksLikeJsShell(doc.plainText())) {
      store.save(doc);
      return new IngestResult(doc, "This page looks JS-rendered (SPA shell). Extracted text may be empty.");
    }

    List<ChunkDto> chunks = chunker.chunk(doc);
    store.save(doc);
    index.index(doc, chunks);

    return new IngestResult(doc, "Saved + indexed (" + chunks.size() + " chunks).");
  }

  public List<Path> listSavedDocs() throws IOException {
    return store.list();
  }

  public DocumentDto loadSavedDoc(Path file) throws IOException {
    return store.load(file);
  }

  public AnswerDto ask(String question) throws IOException, ParseException {
    return answerService.answer(question);
  }

  @Override
  public void close() throws IOException {
    index.close();
  }

  boolean looksLikeJsShell(String plainText) {
    String t = plainText == null ? "" : plainText.strip();
    if (t.length() >= 250) return false;
    String lower = t.toLowerCase();
    return lower.contains("enable javascript")
        || lower.contains("loading")
        || lower.isBlank();
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
