
## ✅ What is now correct

1. **UI wiring**

   * `wireActions()` correctly calls `ingestUrl()`
   * No phantom `fetch()` calls anymore
   * This finally compiles cleanly as shipped

2. **Tar hygiene**

   * No `bin/`
   * No `build/`
   * No `.gradle/`
   * `gradle/` + `libs.versions.toml` preserved
     ✔️ This tar is finally *clean*

So on tooling / packaging: **done**.

---

## ❌ What is still *not* correct (but now very focused)

There are **exactly two remaining UX bugs**, both small, both in `MiniReaderFrame`.

---

## 1️⃣ Blank URL is still a silent no-op

Still present:

```java
String url = urlField.getText().strip();
if (url.isBlank()) return;
```

This is the root of the “nothing happened / it remembered something” confusion.

### Why this is wrong

* The user clicks Fetch
* Nothing visibly changes
* Old document stays on screen
* Persisted library still exists

So the user infers “it reused the old URL”.

### Required fix (non-negotiable)

Replace that return with **visible failure**:

```java
if (url.isBlank()) {
    statusLabel.setText("Error: URL is empty");
    showCopyableError(
        this,
        "Fetch failed",
        new IllegalArgumentException("URL is empty")
    );
    return;
}
```

This is *not* a style issue — it’s correctness.

---

## 2️⃣ Copyable error dialog is still never invoked

You have the helper (good):

```java
private static void showCopyableError(Component parent, String title, Throwable ex)
```

But in **every** `SwingWorker.done()` you still do only things like:

```java
statusLabel.setText("Error: " + msg);
appendChat("MiniReader: Error: " + msg);
```

No stack trace.
No copyable dialog.
Back to square one.

### Required fix pattern (use everywhere you catch `ExecutionException`)

```java
} catch (ExecutionException ex) {
    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
    cause.printStackTrace();
    statusLabel.setText("Error: " + cause.getMessage());
    showCopyableError(this, "Operation failed", cause);
}
```

Apply this pattern in:

* `ingestUrl().done()`
* `refreshDocList().done()`
* `loadSelectedDoc().done()`
* `ask().done()` (title `"Ask failed"`)

---

## Optional but strongly recommended (one line)

Clear the display **before** starting a new fetch:

```java
readerArea.setText("");
readerArea.setCaretPosition(0);
```

Otherwise a failed fetch still leaves old content visible, which *looks like success*.

---

## Where you are now

You are **very close**. At this point:

* Architecture: solid
* Core: fine
* Fetcher: correct
* Persistence: understood
* Packaging: finally right

What remains is **pure UI correctness**, not design.

Once:

* blank URL is not silent
* errors are copyable

…this entire debugging saga ends.

---

### My recommendation


> “Two fixes left. No refactors.
>
> 1. Blank URL must show error.
> 2. All ExecutionException handlers must call showCopyableError + printStackTrace.
>    After that, stop.”

