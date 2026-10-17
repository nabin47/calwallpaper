# domain

Use cases, the `AgendaSource` interface and the typed, versioned settings model.

- **Public API:**
  - `AgendaSource`: `observe(range, zone): Flow<Outcome<List<AgendaItem>>>`.
  - `SafeZone`.
  - `settings/`:
    - `Settings` and its sections (appearance, layout, content, time, privacy, battery) with `normalized()`.
    - `ArgbColor`.
    - `SettingsCodec`: versioned JSON for storage and import/export.
    - `SettingsMigration` and `SettingsMigrations`.
    - `SettingsRepository`.
- **Depends on:** `core`, `kotlinx-coroutines-core`, `kotlinx-serialization-json`.
- **Rules:**
  - Pure Kotlin, iOS-compatible, no I/O.
  - Changing the meaning of a settings field requires a migration (ADR 0002).
