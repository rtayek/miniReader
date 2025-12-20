package app.core;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class CoreFacade implements AutoCloseable {
  public CoreFacade() throws MiniReaderException {
    this(MiniReaderConfig.defaults());
  }

  public CoreFacade(MiniReaderConfig config) throws MiniReaderException {
    try {
      this.config = Objects.requireNonNull(config, "config");
      this.fetcher = new Fetcher();
      this.extractor = new Extractor();
      this.chunker = new Chunker();
      this.store = new DocumentStore(config);
      this.index = new LuceneIndex(config);
      this.answerService = new AnswerService(index);
    } catch (IOException e) {
      throw new MiniReaderException("Failed to initialize core", e);
    }
  }

  public IngestResult ingestUrl(String url) throws MiniReaderException {
    try {
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
    } catch (IOException | ParseException e) {
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
