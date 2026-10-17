# ADR 0007: Todoist sync, token encryption and quick-add

- Status: accepted
- Date: 2026-10-17

## Context
Phase 5 adds Todoist as a source. It also adds two quick ways into the local task list: a Quick Settings tile and a
launcher shortcut. The constraints:

- **Todoist API.** The current API is **v1** (`https://api.todoist.com/api/v1/`). REST v2 and Sync v9 are retired.
  Doist's SDKs fix the version at v1. Third parties report that v2 and v9 return HTTP 410 since February 2026.
  - Lists are cursor-paginated (`results`, `next_cursor`, `limit` ≤ 200).
  - Ids are opaque strings.
  - `due.date` is `YYYY-MM-DD`, `YYYY-MM-DDTHH:MM:SS` (floating), or the same with `Z` (fixed time zone).
- **Token handling.** OAuth needs a client secret and so a backend, which is out of scope. The user pastes a
  personal token. It must be encrypted without the deprecated `security-crypto`, never logged, and never backed up
  (CLAUDE.md §7).
- **Processes.** The wallpaper process must stay light and must not load network code. The app process may.
- **Background work.** It must respect WorkManager's 15-minute minimum, with no polling from the wallpaper
  (CLAUDE.md §5).

## Decision
- **Split by process.**
  - `data/todoist` holds everything Todoist:
    - the Ktor v1 client (`TodoistApi`, on OkHttp);
    - DTOs and their mapper;
    - the sync logic (`TodoistSync`);
    - the offline cache (`DataStoreTodoistCache`);
    - the source (`TodoistSource`).
  - The **sync runs in the app process** (`TodoistSyncWorker`).
  - The **wallpaper reads only the cache**. The cache is a multi-process DataStore in credential-protected storage,
    so a sync reaches the live wallpaper's flow like a settings change does. Ktor stays out of the wallpaper
    process, and the app module never sees Ktor types (`TodoistHttp.sync`).
- **Offline cache, minimal content.**
  - **What is cached:** open, dated tasks (id, title, project, due), projects (id, name, color) and the sync
    status. Completed and undated tasks are dropped: the wallpaper shows days.
  - **Overdue tasks** show on today, as in Todoist's Today view.
  - **Time zones:** a task fixed to a zone is converted to the device's zone; floating times stay local.
  - **Storage:** the cache is excluded from backups. It is deleted when the user disconnects or connects another
    token.
  - **Failures keep the last good data:** the wallpaper keeps showing it offline, and settings shows the error.
- **Token encryption.** `TokenStore` encrypts with an AES-256-GCM key generated in the Android Keystore:
  - The Keystore picks a random IV for each encryption. The file stores `version | ivLength | iv | ciphertext`
    (`SealedSecret`, tested).
  - The file is in `noBackupFilesDir`. A Keystore key cannot be restored anyway, so backing the file up would be
    pointless.
  - A token that no longer decrypts (for example after the screen lock is removed) becomes `TOKEN_UNREADABLE`,
    and settings asks for it again.
  - The plain token exists only in memory during a sync, and `toString` redacts it. Settings never shows it after
    it is saved.
- **Sync schedule and backoff** (`TodoistSyncScheduler`):
  - **Periodic:** unique periodic work every 30 minutes on any network, with policy `UPDATE` (`REPLACE` is
    deprecated).
  - **Immediate:** a unique one-time run when the user connects or taps "Sync now".
  - **Retries:** network, 5xx and 429 failures return `Result.retry()`, with exponential backoff from 60 s, at most
    5 retries per run. WorkManager applies backoff to periodic work too.
  - **No retries:** rejected tokens (401/403), unreadable tokens and unexpected responses (other 4xx including 410,
    unparseable JSON) fail and wait for the user.
  - **After a sync that changed the data,** the battery-saver image and the widgets redraw (`BackgroundRefresh`).
- **Error states in settings.** The cache stores the last attempt, the last success and an error kind, never a
  message from Todoist. Settings maps each kind to a fixed string:
  - "Synced 5 minutes ago";
  - "Couldn't reach Todoist… Last synced …";
  - "Todoist rejected the token. Paste a new one." (the token field returns).
- **Project selection** reuses `ContentSettings.selectedTaskListIds`. Todoist items carry their project id as
  their group.
- **Deep links.** Items open `OpenTarget.TodoistTask` through the router from ADR 0005: the Todoist app's
  `app.todoist.com` link, then `todoist://`, then the web.
- **HTTPS only.** `network_security_config.xml` refuses cleartext for the whole app. `INTERNET` is the permission
  CLAUDE.md §7 already lists.
- **Quick-add** (`QuickAddActivity`, a small Compose dialog: title plus today, tomorrow or no date):
  - **Quick Settings tile** (`QuickAddTileService`): it asks to unlock first on a locked phone, because the task
    list is in credential-protected storage. It opens the dialog through `TileServiceCompat`, which uses the
    PendingIntent API on Android 14+ (the Intent overload throws there).
  - **Static launcher shortcut** (`res/xml/shortcuts.xml`): debug builds have their own copy, because
    `targetPackage` must be a literal and debug adds `.debug`.
  - **Turning the source on:** adding a task turns on "Local tasks" if it was off, so the task is visible.
- **Cross-process invalidation.** Both processes build the local task database with Room's
  `enableMultiInstanceInvalidation()` (Room 3, non-experimental). A quick-added task reaches the live wallpaper
  without a restart. This closes the open point from ADR 0004.

## Permissions
- **New.** No permission beyond CLAUDE.md §7's list is requested by this phase: `INTERNET` is listed there.
- **Already merged.** `androidx.work:work-runtime` declares `WAKE_LOCK`, `ACCESS_NETWORK_STATE`,
  `RECEIVE_BOOT_COMPLETED` and `FOREGROUND_SERVICE` in its own manifest. Glance (ADR 0005) already depends on
  WorkManager, so these were merged into the app from the widgets PR on, which was not noted then.
  - **What they are for:** they are normal permissions with no user prompt. WorkManager needs them for network
    constraints, wake locks during work, rescheduling after boot, and long-running work.
  - **Kept:** removing `FOREGROUND_SERVICE` with `tools:node="remove"` could break expedited work on Android 8 to 11,
    which runs as a foreground service.
  - **Confirmed** in CI (run on f274e14, which now lists the release APK's permissions in every job summary):
    `INTERNET`, `READ_CALENDAR`, `SET_WALLPAPER`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE`,
    `FOREGROUND_SERVICE`, and `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
  - **The last one** is a signature-level permission that androidx.core defines and holds itself. It protects the
    app's own non-exported runtime receivers, so no other app can hold it and the user never sees it.
  - **Optional follow-up:** if the owner prefers, a later change can try removing `FOREGROUND_SERVICE` after
    testing.

## Consequences
- **Needs the phone or a real account to confirm:**
  - the token flow;
  - Todoist's 401/403 behaviour for a bad token;
  - the `todoist://` link;
  - tile and shortcut behaviour on the owner's launcher.
- **Freshness.** Todoist changes reach the wallpaper within about 30 minutes, sooner with "Sync now". There are no
  push updates; webhooks would need a server.
- **Sync size.** Each sync fetches all active tasks: one request per 200 tasks, plus projects. That is far below
  Todoist's rate limits at this period.
- **Todoist's undated tasks** are not shown. Recurring tasks show their next occurrence only.
