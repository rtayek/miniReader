package app.core;

public sealed interface IngestOutcome
    permits IngestOutcome.SavedIndexed,
            IngestOutcome.JsShell,
            IngestOutcome.RejectedNonHtml,
            IngestOutcome.HttpError,
            IngestOutcome.FetchError {

  record SavedIndexed(DocumentDto doc, int chunkCount) implements IngestOutcome {}

  record JsShell(DocumentDto doc, String reason) implements IngestOutcome {}

  record RejectedNonHtml(String contentType) implements IngestOutcome {}

  record HttpError(int statusCode, String snippet) implements IngestOutcome {}

  record FetchError(String message) implements IngestOutcome {}
}
