# data

Persistence and agenda sources.

- **Public API:**
  - `calendar/`:
    - `CalendarProviderSource`: Android Calendar Provider, with a `ContentObserver` and no polling. Needs `READ_CALENDAR`.
    - `CalendarInstanceRow` and its pure mapper.
    - `DeviceCalendars`: the list of visible calendars, for per-calendar selection in settings.
  - `local/`:
    - `LocalTasksSource` (read).
    - `LocalTasks` (quick-add, complete, delete).
    - `LocalTasksDatabase` (Room 3) and `LocalTasksDatabaseFactory`, with multi-instance invalidation across the
      app and wallpaper processes.
  - `todoist/` (ADR 0007):
    - `TodoistApi`: the Todoist API v1 client on Ktor, with pagination and error mapping.
    - `TodoistHttp.sync`: one sync on OkHttp, so callers never see Ktor.
    - `TodoistSync`: the sync logic.
    - `TodoistMapper` and `TodoistColors`: wire objects to cache.
    - `DataStoreTodoistCache`: the multi-process offline cache in credential-protected storage.
    - `TodoistSource`: reads the cache only.
    - `TodoistToken` and `TodoistTokenFormat`.
  - `secure/`:
    - `TokenStore`: a Keystore AES-256-GCM secret in `noBackupFilesDir`.
    - `SealedSecret`: the on-disk format.
    - `TokenRead`.
  - `settings/`: `DataStoreSettingsRepository`, stored in device-protected storage for Direct Boot and opened with
    `MultiProcessDataStoreFactory`, so the app and the `:wallpaper` process see each other's changes (ADR 0004).
- **Depends on:** `core`, `domain`, Room 3 (KSP), `androidx.sqlite` (platform driver), DataStore, Ktor (client core;
  OkHttp engine on Android; mock engine in tests).
- **Build:**
  - KMP with an Android target (`com.android.kotlin.multiplatform.library`, `kotlin { android { } }`).
  - Host tests live in `src/androidHostTest`.
  - Room schemas live in `schemas/`. They must be committed; CI checks this.
- **Rules:**
  - Never log tokens or event and task titles.
  - Secrets are stored encrypted (Keystore AES-GCM or Tink).
  - Create the database and the settings repository once per process.
