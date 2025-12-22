package app.core;

import app.core.internal.CoreRuntime;
import app.core.internal.CoreRuntimes;

import java.util.List;
import java.util.Objects;

public class CoreFacade implements AutoCloseable {
  public CoreFacade() throws MiniReaderException {
    this(MiniReaderConfig.defaults());
  }

  public CoreFacade(MiniReaderConfig config) throws MiniReaderException {
    this.runtime = CoreRuntimes.create(Objects.requireNonNull(config, "config"));
  }

  public IngestOutcome ingestUrl(String url) throws MiniReaderException {
    return runtime.ingestUrl(url);
  }

  public List<SavedDocDto> listSavedDocs() throws MiniReaderException {
    return runtime.listSavedDocs();
  }

  public DocumentDto loadSavedDoc(String id) throws MiniReaderException {
    return runtime.loadSavedDoc(id);
  }

  public AnswerDto ask(String question) throws MiniReaderException {
    return runtime.ask(question);
  }

  @Override
  public void close() throws MiniReaderException {
    runtime.close();
  }

  private final CoreRuntime runtime;
}
