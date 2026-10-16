# domain

Use cases, the `AgendaSource` interface, and the typed settings model.

- **Public API (Phase 0):** `SafeZone` (screen-height fractions the layout keeps clear).
- **Depends on:** `core`.
- **Rules:** pure Kotlin, iOS-compatible, immutable models, no I/O. Adding a source must not change this module beyond the interface.
