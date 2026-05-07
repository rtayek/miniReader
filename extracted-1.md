Here is the **durable semantic extraction** for this chat using your template. 

---

# Active Semantic State

## Stable Decisions

Decisions likely to remain valid.

* The project is a **library-first architecture** with a Swing UI acting strictly as a client.
* `CoreFacade` is the **only public entry point** into the core.
* The public API exposes **DTOs and IDs**, not filesystem constructs (`Path` removed).
* Ingestion uses a **typed outcome model (`IngestOutcome`)** instead of nulls or string signaling.
* **Internal failures throw exceptions** (`MiniReaderException`), while **expected external failures return outcomes**.
* Persistent storage is located at `~/.miniReader` and is part of the system model.
* UI is responsible for **error rendering and user feedback**, not core logic.

---

## Constraints

Architectural, workflow, tooling, semantic, compatibility, or style constraints.

* No leakage of:

  * Lucene
  * filesystem paths
  * HTTP/parsing internals
    into the public API.
* UI must remain **read-only with respect to core state** (DTO-driven).
* Avoid:

  * `Optional` for failure signaling
  * string-based error interpretation
* Keep architecture **simple and local-first** (no premature web/mobile abstractions).
* Tar artifacts must exclude build/cache directories (`.gradle`, `bin`, `build`).
* Prefer **exceptions for internal faults** and **explicit outcomes for expected states**.

---

## Active Model

Current conceptual model being used.

* The system is a **pipeline-oriented ingestion and query engine**:

  ```
  URL
   → Fetcher
   → Validation
   → Extractor
   → Shell detection
   → Chunking
   → Storage
   → Indexing
   → Query/Answer
  ```

* The UI is a **client/controller**:

  * invokes `CoreFacade`
  * renders DTOs
  * handles errors

* The system is effectively a **local document ingestion + search + summarization engine**.

---

## Reusable Patterns

Patterns, idioms, workflows, or decision rules worth reusing.

* **Facade pattern** for enforcing a single public boundary.
* **DTO boundary pattern** for UI isolation and future remoting.
* **Outcome + Exception split**:

  * outcomes = expected external states
  * exceptions = internal/system faults
* **Pipeline composition** for ingestion stages.
* **Single error choke point in UI** with copyable diagnostics.
* **Semantic refactoring discipline**:

  * prefer correctness and clarity over abstraction
  * avoid premature generalization

---

## Open Questions

Still unresolved.

* Should JS-shell pages:

  * be saved only (current), or
  * be saved and indexed with degraded ranking?
* Should `FetchError` be enriched (typed error categories vs message + class name)?
* How far to push **ShellHeuristics sophistication** vs simplicity?
* Whether to introduce a **second client (CLI or remote)** to validate architecture pressure.

---

## Rejected / Deferred

Intentionally rejected, postponed, or explicitly out of scope.

* Premature abstraction of Lucene behind a pluggable backend.
* Event/observer systems.
* Async core APIs beyond current needs.
* Optional/monadic error handling.
* Designing for web/mobile clients before real need.
* Over-generalization of ingestion pipeline.

---

# Suggested Canonical Updates

## architecture.md

Durable structural decisions, layer boundaries, dependency rules, major abstractions.

* Core/UI separation with `CoreFacade` as the only public API.
* DTO-only boundary; no filesystem or engine leakage.
* Pipeline-based ingestion model.
* Explicit distinction between:

  * external failures → `IngestOutcome`
  * internal failures → exceptions

---

## design.md

Current design intent, UX behavior, object responsibilities, interaction flows.

* UI behavior:

  * clear display before fetch
  * copyable error dialogs
  * explicit feedback for blank URL and failures
* JS-shell handling:

  * detect, save, do not index (current policy)
* Persistent library model:

  * documents accumulate in `~/.miniReader`
  * UI reflects this explicitly

---

## patterns.md

Reusable implementation or reasoning patterns.

* Outcome vs exception separation
* DTO boundary enforcement
* Pipeline stage composition
* “Single choke point” error handling
* Semantic compression workflow for knowledge extraction

---

## working-context.md

Temporary but still useful project state: current direction, next likely work, recent unresolved issues.

* Architecture is **stable and complete**
* Remaining work is **semantic refinement**, not structural:

  * XHTML handling confirmed
  * internal vs external failure separation mostly complete
* Next logical steps:

  * optional CLI client
  * real usage testing
  * possible remote protocol design

---

# Compression Guidance

* This project has transitioned from **exploratory design → stable architecture**.
* Further changes should be **driven by usage**, not refactoring.
* The only remaining meaningful changes are **semantic correctness and feature expansion**, not structure.

---
