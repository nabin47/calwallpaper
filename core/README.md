# core

Pure Kotlin Multiplatform foundation: models, time utilities, result types.

- **Public API:** `Outcome` (`Success`/`Failure`, `map`, `getOrNull`).
- **Depends on:** nothing.
- **Rules:** no `java.*`, no Android APIs, no third-party deps except kotlinx (coroutines, datetime, serialization). It must stay iOS-compatible.
