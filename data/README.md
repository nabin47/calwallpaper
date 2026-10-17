# data

Persistence and sources: Room 3, DataStore, Ktor (OkHttp engine), and `AgendaSource` adapters
(`CalendarProviderSource`, `LocalTasksSource`, `TodoistSource`).

- **Public API (Phase 0):** `SourceId` (stable persisted keys for sources).
- **Depends on:** `core`, `domain`.
- **Build:** KMP with an Android target (`com.android.kotlin.multiplatform.library`, `kotlin { android { } }`); host tests in `src/androidHostTest`.
- **Rules:** never log tokens or event/task titles. Secrets are stored encrypted (Keystore AES-GCM or Tink).
