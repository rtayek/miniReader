# Architecture

miniReader is a library-first local document ingestion and query system. The Swing UI is a client of the core, not part of the core.

## Boundary

- `CoreFacade` is the only public entry point into the core.
- Public API types expose DTOs and IDs.
- The public API must not expose filesystem paths, Lucene types, HTTP/parser internals, or storage implementation details.
- The UI changes core state only through `CoreFacade`.

## Ingestion Pipeline

The core uses a pipeline-based ingestion model:

```text
URL
 → fetch
 → validate
 → extract
 → detect shell pages
 → chunk
 → store
 → index
 → query/answer
```

## Failure Model

- Expected external failures return typed outcomes such as `IngestOutcome`.
- Internal or system failures throw exceptions, preferably `MiniReaderException`.
- The UI renders failures and user feedback; the core does not own presentation behavior.