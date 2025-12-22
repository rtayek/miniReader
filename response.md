Checked it. **This one finally passes.** ✔️

### What’s correct now

* ✅ **Tar hygiene is clean**

  * No `bin/`
  * No `build/`
  * No `.gradle/`
  * `gradle/` + `libs.versions.toml` preserved
    → This archive is portable and won’t resurrect stale state.

* ✅ **UI behavior is sane**

  * Blank URL → visible, copyable error
  * Errors everywhere → stack trace + copyable dialog
  * New Fetch clears old content first
  * No phantom `fetch()` wiring issues

* ✅ **Persistence model is clear**

  * Library persists in `~/.miniReader`
  * UI no longer lies about what was fetched “this session”

### Net result

The original pain points are actually gone:

* no “haunted” behavior
* no silent no-ops
* no uncopyable errors
* no stale build artifacts sneaking in

This is the point where you **stop iterating on this loop**.

### Recommendation

Tag this state. You’ve earned it:

```bash
git tag -a v0.2-ui-sane -m "UI fixes: copyable errors, no silent fetch, clean tar, sane startup"
git push origin v0.2-ui-sane
```

From here on, any further changes should be **feature-driven**, not cleanup-driven:

* Clear Library button
* Ask scope toggle (selected doc vs all)
* Remote/CLI client
* Better doc list metadata

But as far as this debugging saga goes: **done**.

second prompt:

Stop leaking Path through the public API

Right now CoreFacade exposes:

List<Path> listSavedDocs()

DocumentDto loadSavedDoc(Path file)

That’s fine for a local Swing app, but it’s a dead end for:

remote player/client

web/mobile

even a CLI you run on a different machine

Refactor: replace Path with a small DTO or opaque ID.

Example direction:

List<SavedDocDto> listSavedDocs() where SavedDocDto contains:

id, title, url, fetchedAt, maybe summary

DocumentDto loadSavedDoc(String id) (or loadSavedDoc(SavedDocRefDto ref))

This turns your core into an actual library/service boundary instead of “UI helper around a filesystem”.

2) Make IngestResult a real result type (no null doc, no stringly messages)

You currently do the classic “doc might be null; message explains why” thing.

That works, but it’s sloppy for consumers and will metastasize.

Refactor: use a sealed/enum-ish outcome.

Example shape:

IngestOutcome.savedIndexed(doc, chunkCount)

IngestOutcome.rejectedNonHtml(reason)

IngestOutcome.jsShell(doc, reason)

IngestOutcome.httpError(code, snippet)

Now the UI (or remote client) can switch on outcomes instead of parsing message strings and checking for null.

3) Clean up Fetcher: reuse HttpClient and delete debug prints

Your Fetcher.fetch() currently:

rebuilds HttpClient every call

prints request/code/content-type with System.out.println + flush

That’s fine for debugging but not architecture.

Refactor:

HttpClient becomes a field created once in the constructor

replace prints with either:

nothing (preferred), or

a tiny Logger interface / listener (if you want UI status updates later)

This will make behavior more consistent and removes “why is it printing sometimes” confusion.

Honorable mention: move URL canonicalization + ID generation into a factory

Right now Extractor is doing invariants work (canonicalizing URL, hashing, etc.). That’s okay, but it’s a hidden contract.

If you want to tighten correctness:

DocumentDto gets a static DocumentDto.fromExtracted(...) factory that computes/validates id/url invariants.

Not urgent, but it’s a good “make invalid states unrepresentable” step.
