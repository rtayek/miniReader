I pulled the tar apart and reviewed it as “what did Evil Twin actually change, what’s good, what’s still architecturally off, and what’s the next baby steps.”

## What Evil Twin did well

### UI is now operationally sane

`MiniReaderFrame` is in a good place:

* Fetch runs off the EDT (`SwingWorker`) and clears the display before work starts.
* Blank URL is handled visibly (no silent no-op).
* All `ExecutionException` paths print stack traces and show a copyable dialog.
* `IngestOutcome` is handled explicitly and drives UI behavior (good boundary discipline).
* The saved-doc list is now DTO-based (`SavedDocDto`) and loads by `id` (good “future remote client” posture).

Net: the “haunted UI” phase is behind you.

### Fetcher is correct

`Fetcher` now correctly reuses a single `HttpClient` instance. That’s the right default.

## The main architectural problem that remains

### CoreRuntime still collapses internal failures into `FetchError`

In `CoreRuntime.ingestUrl(String)`:

```java
} catch (Exception e) {
  ...
  return new IngestOutcome.FetchError(msg, e.getClass().getSimpleName());
}
```

That catch-all is still doing damage. It turns **everything** into “fetch error,” including:

* `DocumentStore.save` failures (disk, serialization, permissions)
* `LuceneIndex.index` failures
* extractor bugs
* chunker bugs
* anything else internal

This will mislead you during real use, and it will be painful when you add a second client.

### Recommended fix: split “expected outcomes” vs “internal faults”

Keep `IngestOutcome` for expected ingest outcomes:

* non-2xx HTTP
* non-HTML content type
* “JS shell” detection
* network / URI / timeout / SSL / interruption

But for **internal** failures, throw `MiniReaderException` (your public API already supports `throws MiniReaderException`).

Baby-step implementation (conceptual):

* Catch: `IllegalArgumentException` (bad URI), `IOException`, `InterruptedException` from the fetch path → return `FetchError`
* Let `IOException`/`RuntimeException` from store/index/extract/chunk be wrapped and thrown as `MiniReaderException`

That gives your UI the ability to say:

* “site blocked you” vs
* “your local library/index is broken”

Right now it cannot.

## One real correctness bug in core

### XHTML is rejected

`validateFetch` currently checks only for `text/html` and rejects XHTML:

```java
if (!fetch.contentType().toLowerCase().contains("text/html") && !fetch.contentType().isBlank()) {
  return new IngestOutcome.RejectedNonHtml(fetch.contentType());
}
```

But your `Accept` header explicitly includes `application/xhtml+xml`. So this is inconsistent.

Baby-step fix:

* treat `application/xhtml+xml` as HTML-ish as well.

## Boundary hardening opportunities (low risk, nice cleanup)

### 1) `CoreFacade` stores config but never uses it

`private final MiniReaderConfig config;` in `CoreFacade` is dead. Remove it unless you plan to expose it.

### 2) `CoreRuntime` is public in an internal package

`CoreRuntime` is `public` but constructed package-private. It’s not a crisis, but it’s an attractive nuisance.

If you want to harden properly without contortions:

* introduce a `public interface CoreRuntimeApi` in `app.core.internal`
* make `CoreRuntime` package-private implementing it
* `CoreFacade` holds the interface type

### 3) `looksLikeJsShell` shouldn’t be a method on the runtime

It’s a classifier/heuristic. Move it to a tiny internal helper (`ShellDetector`) or into `Extractor` if you treat it as part of extraction quality.

## Tar hygiene regression

This tar includes a top-level `/.gradle` directory again. Not a code issue, but it’s junk that reintroduces confusion.

## Recommended next steps (in order)

1. Fix XHTML acceptance in `validateFetch`.
2. Split `ingestUrl` error handling: outcomes for expected failures, exceptions for internal faults.
3. Remove dead `config` field from `CoreFacade`.
4. Optional: harden internal boundary (`CoreRuntimeApi` interface), and relocate JS-shell detection helper.
5. Fix tar script to exclude `.gradle/` consistently.

If you want, paste the current `CoreRuntime.ingestUrl` and I’ll give you the exact “small patch” version of the exception/outcome split (minimal churn, keeps UI unchanged except for one more error path).
