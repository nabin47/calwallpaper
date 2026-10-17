# data

Persistence and agenda sources.

- **Public API:**
  - `calendar/`:
    - `CalendarProviderSource`: Android Calendar Provider, with a `ContentObserver` and no polling. Needs `READ_CALENDAR`.
    - `CalendarInstanceRow` and its pure mapper.
  - `local/`:
    - `LocalTasksSource` (read).
    - `LocalTasks` (quick-add, complete, delete).
    - `LocalTasksDatabase` (Room 3) and `LocalTasksDatabaseFactory`.
  - `settings/`: `DataStoreSettingsRepository`, stored in device-protected storage for Direct Boot.
- **Depends on:** `core`, `domain`, Room 3 (KSP), `androidx.sqlite` (platform driver), DataStore.
- **Build:**
  - KMP with an Android target (`com.android.kotlin.multiplatform.library`, `kotlin { android { } }`).
  - Host tests live in `src/androidHostTest`.
  - Room schemas live in `schemas/`. They must be committed; CI checks this.
- **Rules:**
  - Never log tokens or event and task titles.
  - Secrets are stored encrypted (Keystore AES-GCM or Tink).
  - Create the database and the settings repository once per process.
