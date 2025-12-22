package app.core.internal;

import app.core.AnswerDto;
import app.core.DocumentDto;
import app.core.IngestOutcome;
import app.core.MiniReaderException;
import app.core.SavedDocDto;

import java.util.List;

public interface CoreRuntimeApi extends AutoCloseable {
  IngestOutcome ingestUrl(String url) throws MiniReaderException;

  List<SavedDocDto> listSavedDocs() throws MiniReaderException;

  DocumentDto loadSavedDoc(String id) throws MiniReaderException;

  AnswerDto ask(String question) throws MiniReaderException;

  @Override
  void close() throws MiniReaderException;
}
