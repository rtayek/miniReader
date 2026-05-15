# Design

## UI Behavior

- Clear the display before starting a fetch.
- Provide explicit feedback for blank URLs and fetch failures.
- Present copyable error dialogs for diagnostics and debugging.

## JS-Shell Handling

Current policy:

- detect shell pages
- save them
- do not index them

## Persistent Library Model

- Documents accumulate in `~/.miniReader`.
- The UI explicitly reflects the persistent local-library model rather than treating documents as temporary session state.