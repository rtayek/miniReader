Implemented the requested cleanup and polish:

Removed the facade leak: CoreFacade no longer exposes looksLikeJsShell; the check stays internal.
Enriched ingest failures: IngestOutcome.FetchError now carries the exception class, and CoreRuntime populates it; UI shows the richer details in the copyable dialog.
Centralized magic numbers: added constants for JS-shell max length, error snippet length, readability minimum length, and inline code length.
Improved saved-doc list display: SavedDocDto.toString() renders title — url for clearer UI lists.
Fetcher already reuses a single HttpClient with no debug prints.
Remaining tracked changes (per git status): chatgpt.md, core/runtime/outcome updates, and UI wiring. No tests were run here; recommend ./gradlew :core:test to verify.