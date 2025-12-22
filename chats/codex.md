Implemented the requested architectural fixes:

Core runtime contract: Added CoreRuntimeApi and have CoreFacade depend on the API; CoreRuntime is now package-private and implements it.
Error semantics: CoreRuntime.ingestUrl now returns FetchError only for fetch/IO failures; interruptions throw MiniReaderException; downstream (store/index/extract) failures also surface as MiniReaderException. No more collapsing internal faults into FetchError.
Content-type fix: validateFetch now accepts application/xhtml+xml as HTML.
Leak cleanup: Removed the facade’s internal helper leak (looksLikeJsShell is internal via ShellHeuristics), and the unused config field is gone.
Tests updated: Shell detection test now uses ShellHeuristics directly, no runtime leak.
Note: .gradle/ should be excluded when tarring; current code changes include the new CoreRuntimeApi and the updated classes/tests above. Please run ./gradlew :core:test to verify.