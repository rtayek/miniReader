Yep — I unpacked the tar and did an architecture-focused pass.

## What you’ve already refactored (and it’s the right direction)

### 1) You *did* the “remove `Path` from the public API” refactor

`CoreFacade` now exposes:

* `List<SavedDocDto> listSavedDocs()`
* `DocumentDto loadSavedDoc(String id)`

…and `SavedDocDto` is a clean record (`id/title/url/fetchedAt`). This is the single biggest “future remote client” enabler.

### 2) Ingest now returns a real outcome type

`IngestOutcome` is a sealed interface with explicit variants:

* `SavedIndexed(doc, chunkCount)`
* `JsShell(doc, reason)`
* `RejectedNonHtml(contentType)`
* `HttpError(statusCode, snippet)`
* `FetchError(message)`

That’s exactly the kind of “don’t make the UI parse strings / infer null” boundary you want.

### 3) Fetcher is no longer doing dumb per-call client creation

`Fetcher` holds a configured `HttpClient` as a field. Good.

## What’s still worth tweaking (small, but real)

### A) Remove `CoreFacade.looksLikeJsShell(...)`

`CoreFacade` has this package-private method:

```java
boolean looksLikeJsShell(String plainText) { ... }
```

Now that `ingestUrl()` can return `IngestOutcome.JsShell`, you don’t need a separate “peek inside internals” method on the facade at all.

**Recommendation:** delete it (or move it fully internal).
It’s a “test helper leak” and it’ll nag you later when you add more clients.

### B) Make `FetchError` carry more signal (optional)

Right now it’s just `FetchError(String message)`.

Consider one of:

* `FetchError(String message, String exceptionClass)` (minimal)
* or include a short “kind” enum (dns/ssl/timeout/etc.) if you find UI wants nicer messages

Don’t over-engineer it; just stop losing the cause type entirely.

## Packaging hygiene note

This tar still includes a top-level `./.gradle` directory (Gradle cache). It’s not a code architecture issue, but it’s still junk-in-archive.

If you want it gone for good, you already know the fix: exclude **both** the directory and its contents.

---

## Bottom line

No “major” refactorings left that are mandatory. You’ve already knocked out the big ones (API boundary + outcome typing). The remaining improvements are cleanup/polish:

1. kill `CoreFacade.looksLikeJsShell`
2. optionally enrich `FetchError`
3. stop shipping `.gradle/` in tars

If you tell me “remote player next”, the next architectural step is designing the wire protocol around `SavedDocDto`, `DocumentDto`, `AnswerDto`, and `IngestOutcome`—you’re basically already set up for it.
