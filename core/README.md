# core

Pure Kotlin Multiplatform foundation: agenda models, time utilities, result types.

- **Public API:**
  - `AgendaItem`, `ItemKey`, `ItemKind` and `ItemTime` (`AllDay`, `Timed`, `Due`).
  - `OpenTarget`: where a tap leads.
  - `OpenTargetLink`: an `OpenTarget` as a `calwallpaper://open/…` link and back, plus the router's class name
    (ADR 0005).
  - `SourceId`: stable keys for each source.
  - `Outcome`.
  - `time/`: `DateRange`, `InstantRange`, `ItemTime.overlaps` / `startDate` / `sortInstant`, and `agendaOrder(zone)`.
- **Depends on:** `kotlinx-datetime`, `kotlinx-serialization-core` (both exposed as `api`).
- **Rules:**
  - No `java.*` and no Android APIs, so the module stays iOS-compatible.
  - Models are immutable.
  - Titles are user content: never log them. `AgendaItem.toString` redacts them.
