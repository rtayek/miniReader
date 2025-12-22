Current state quick analysis:

API cleanup is in place: SavedDocDto boundary, IngestOutcome sealed result, Path no longer leaks across CoreFacade, Fetcher reuses a single HttpClient, magic numbers centralized.
UI consumes SavedDocDto and switches on IngestOutcome; error dialogs are copyable and blank-URL is handled.
One repo change stands out: response.md is staged for deletion; undo or commit intentionally.
Tests haven’t been run here since the refactor; recommend ./gradlew :core:test to validate.
If you want me to verify or restore response.md, let me know.