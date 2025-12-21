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
