package app.core;

import app.core.internal.CoreRuntime;
import app.core.internal.CoreRuntime.IngestResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class CoreFacade implements AutoCloseable {
  public CoreFacade() throws MiniReaderException {
    this(MiniReaderConfig.defaults());
  }

  public CoreFacade(MiniReaderConfig config) throws MiniReaderException {
    this.config = Objects.requireNonNull(config, "config");
    this.runtime = CoreRuntimes.create(config);
  }

  public IngestResult ingestUrl(String url) throws MiniReaderException {
    CoreRuntime.IngestResult r = runtime.ingestUrl(url);
    return new IngestResult(r.doc(), r.message());
  }

  public List<Path> listSavedDocs() throws MiniReaderException {
    return runtime.listSavedDocs();
  }

  public DocumentDto loadSavedDoc(Path file) throws MiniReaderException {
    return runtime.loadSavedDoc(file);
  }

  public AnswerDto ask(String question) throws MiniReaderException {
    return runtime.ask(question);
  }

  @Override
  public void close() throws MiniReaderException {
    runtime.close();
  }

  boolean looksLikeJsShell(String plainText) {
    return runtime.looksLikeJsShell(plainText);
  }

  public record IngestResult(DocumentDto doc, String message) {}

  private final CoreRuntime runtime;
  private final MiniReaderConfig config;
}
