I unpacked the tar and reviewed **core + UI** with an “architectural leverage” lens (what will matter as you add features or a second client). You’re in a good place: the project is coherent, testable, and the DTO/outcome boundary is doing real work.

Below are the **highest-value recommendations**, in baby steps, with concrete targets.

## 1) Fix a correctness bug: XHTML is rejected

`CoreRuntime.validateFetch(...)` currently treats anything not containing `"text/html"` as non-HTML, which incorrectly rejects `application/xhtml+xml` (even though your `Accept` header includes it).

**Change:** allow XHTML as “HTML-ish”.

Why it matters: some sites legitimately serve XHTML, and you’ll get false negatives.

## 2) Stop collapsing internal failures into `IngestOutcome.FetchError`

Right now `CoreRuntime.ingestUrl(...)` has a catch-all:

```java
} catch (Exception e) { ... return new IngestOutcome.FetchError(...); }
```

This mislabels:

* store/save failures
* Lucene indexing failures
* extraction/chunking bugs

…as “fetch error”.

**Recommendation:** split failure semantics:

* return `IngestOutcome.*` for *expected/external* failures (HTTP status, non-HTML, JS shell, network/URI issues)
* throw `MiniReaderException` for *internal/system* failures (store/index/extract/chunk)

Why it matters: it’s the difference between “site blocked me” and “my local library/index is broken”. This becomes critical the moment you add a remote client or even just want reliable diagnostics.

## 3) Remove dead fields and tighten boundaries

These are low-risk cleanups that reduce confusion:

* `CoreFacade` stores `private final MiniReaderConfig config;` but never uses it after construction. Remove it unless you plan to expose config.
* `CoreRuntime` is `public` in `app.core.internal` but has a package-private constructor. Make `CoreRuntime` package-private (`class CoreRuntime`) and keep access through `CoreRuntimes`. That hardens the “only via facade” rule.

## 4) Move JS-shell detection out of `CoreRuntime`

`looksLikeJsShell(String)` doesn’t belong to “runtime”; it’s content classification. Move it to a small internal helper (e.g., `ShellDetector`) or into `Extractor` if you consider it part of extraction quality.

Why it matters: cohesion. It keeps `CoreRuntime` focused on orchestration.

## 5) Clean up `DocumentStore.list()` (minor but easy)

`list()` sorts twice:

```java
.sorted()
...
.sorted(Comparator.comparing(...).reversed())
```

The first `.sorted()` is redundant. Remove it.

Also, when JSON parsing fails, `toSavedDoc()` currently uses the filesystem path as the title. Consider making that obviously an error (e.g., title = `"Unreadable document"` and store the path in url or an error field) so the UI doesn’t display nonsense.

## 6) Tar hygiene: you still ship an empty `./.gradle/` entry

The archive includes `./.gradle/` (directory entry only). Not fatal, but if your goal is “no `.gradle` at all,” update the tar excludes to remove the directory itself, not just its contents.

---

# If you do only two things

1. **Fix XHTML acceptance** in `validateFetch`.
2. **Split internal failures vs external failures** (exceptions vs `IngestOutcome`).

Those two changes give you the biggest jump in correctness + debuggability with minimal churn.

If you want, I can draft the exact patch for #1 and a minimal, non-invasive patch for #2 that keeps your UI code almost unchanged.
