# Google Play readiness

Draft answers to prepare before the first Play upload (CLAUDE.md §10). Fill in as features land.

- **applicationId:** `io.github.nabin47.calwallpaper` (permanent).
- **Target SDK:** 37 (Play requires ≥ 36 for new apps and updates since 2026-08-31).
- **Privacy policy URL:** https://nabin47.github.io/calwallpaper/privacy/

## Data safety (draft)
- Data collected: **none**. Data shared: **none**.
- Calendar events are read on-device only, to render the wallpaper and widgets.
- Todoist API token: stored encrypted on-device, sent only to Todoist over HTTPS.
- Encryption in transit: yes (HTTPS only). Account creation: none. Data deletion: uninstalling removes all data.

## Permission declarations
- `READ_CALENDAR`: core functionality (shows the user's events on the wallpaper/widgets), requested in context with a rationale screen.
- `INTERNET`: only for sources the user enables (Todoist).
- `SET_WALLPAPER` (normal, no prompt): battery saver mode sets a still image of the agenda as the wallpaper.
- `RECEIVE_BOOT_COMPLETED` (normal, no prompt): battery saver mode and the widgets re-arm their update triggers
  after a reboot. The receiver is disabled while neither is in use.
- No `QUERY_ALL_PACKAGES`, no exact alarms, no foreground service.
- Package visibility: `<queries>` lists only the intents taps open: calendar day and event views, the `todoist`
  scheme and `https` browsers (ADR 0005). No package-visibility declaration is needed in the Play Console.

## First upload
Google Play requires the first AAB to be uploaded manually in the Play Console. After that, finish
`.github/workflows/play.yml` (service-account JSON as a secret) and set the variable `PLAY_UPLOAD_ENABLED=true`.
