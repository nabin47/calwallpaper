# CLAUDE.md — CalWallpaper

Persistent project rules. Read this fully before every task. Keep it updated when decisions change.

## 1. Product

CalWallpaper is an Android app that renders the user's calendar events and tasks onto the **home and lock-screen wallpaper** (live wallpaper, plus a battery-saver static mode) and also ships **Glance home-screen widgets**. Tapping a day or an item opens the matching page in the source app (Google Calendar, Todoist, etc.) where the platform allows it. iOS comes later; Google Play release is planned.

Design: minimal, modern, calm. Typography and spacing do the work; no clutter, no gradients-for-the-sake-of-it.

## 2. Platform reality (design around these, do not fight them)

- **Lock screen is not tappable.** Android never delivers touches on the lock screen to a wallpaper. Lock-screen = read-only display. Interaction is home screen (live wallpaper tap commands, launcher-dependent) and Glance widgets (always works).
- Live wallpaper tap delivery (`WallpaperService.Engine.onCommand` with `android.wallpaper.tap`) depends on the launcher. Verify on a real device; keep widgets as the guaranteed-interactive path.
- Lock-screen clock and notifications overlap the wallpaper: the layout needs a configurable **safe zone** (top/bottom insets) and presets that avoid the clock area.
- Lock screen is visible to anyone: provide **privacy mode** (hide task titles while the device is locked, i.e. `KeyguardManager.isDeviceLocked` on secure devices (ADR 0004), show counts or dots only).
- Wallpaper can run before first unlock (Direct Boot). Credential-protected storage (Room DB) is unavailable then. Persist the last rendered scene or bitmap in device-protected storage and render from that.
- iOS cannot set wallpapers programmatically. The iOS plan is WidgetKit (Lock Screen + Home widgets) on shared Kotlin code. Do not promise wallpaper parity on iOS.

## 3. Stack (decided)

- **Kotlin Multiplatform** for shared logic; **native Android** for wallpaper service and widgets. No Flutter or React Native (engine overhead contradicts the RAM goal).
- Kotlin (latest stable), coroutines + Flow, kotlinx.serialization, kotlinx.datetime.
- Persistence: Room 3 (`androidx.room3`, KMP) with KSP, and DataStore for settings. No `kapt`. (ADR 0001)
- Networking: Ktor client (OkHttp engine on Android).
- Settings UI: Jetpack Compose (Material 3). **The wallpaper process must not load Compose.**
- Widgets: Jetpack Glance.
- DI: manual constructor injection (decided in ADR 0001; no Koin). No reflection-heavy setup in the wallpaper path.
- Build: Gradle Kotlin DSL, version catalog (`libs.versions.toml`), convention plugins in `build-logic/`, R8 full mode, resource shrinking.
- `minSdk` 26. `targetSdk`/`compileSdk` = latest stable level required by Google Play at build time. Verify, do not assume. (2026-10: 37; Play requires ≥ 36.)
- Identity: app name **CalWallpaper**, `applicationId` **`io.github.nabin47.calwallpaper`** (permanent), license Apache-2.0.
- Lint tooling exception: detekt is pinned to a 2.0 alpha because no stable detekt supports Kotlin 2.4 (ADR 0001). Move to stable when released.

**Dependency rule:** before adding any library or plugin, check its latest stable version and deprecation status on Maven Central / official docs (use web search). Never pin from memory. Never use deprecated or archived libraries or APIs, for example: `kapt`, `AsyncTask`, `EncryptedSharedPreferences`/`security-crypto` (deprecated), Gson, `startActivityForResult`, `onBackPressed`, `LiveData` in new code, `SharedPreferences` for new settings, `jcenter`. Fix deprecation warnings; do not suppress them. Treat warnings as errors in CI.

## 4. Architecture

```
build-logic/            convention plugins
core/                   KMP: models, time utils, result types (no deps on Android)
domain/                 KMP: use cases, AgendaSource interface, settings model
render/                 KMP: layout engine -> Scene (list of draw primitives + hit regions)
data/                   KMP: Room, DataStore, Ktor, source adapters
android-wallpaper/      WallpaperService + Canvas renderer (NO Compose, NO DI framework if avoidable)
android-widgets/        Glance widgets
app/                    Settings UI, onboarding, permissions, deep-link router
baselineprofile/        Test-only: generates the app's Baseline Profile in CI (ADR 0008)
site/                   Static landing page + privacy policy (GitHub Pages)
docs/                   Architecture, ADRs, contributing, release process
```

Key ideas:
- **One Scene, many renderers.** `render` turns (agenda items + settings + screen size + insets + privacy state) into a platform-neutral `Scene`: text runs, rounded rects, and hit regions that carry an `OpenTarget`. The Android Canvas renderer, the settings live-preview, the Glance widgets and (later) iOS all consume it. Layout logic is written once and unit-tested on the JVM.
- **`AgendaSource` interface** with adapters: `CalendarProviderSource` (Android Calendar Provider, no OAuth; covers Google Calendar synced on device), `LocalTasksSource`, `TodoistSource`, later `GoogleTasksSource`. Adding a source must not touch the render or wallpaper modules.
- **`OpenTarget`** is a sealed type (CalendarDay, CalendarEvent, TodoistTask, TodoistProject, LocalItem, Url). Android resolves it to an Intent, with a fallback to the web URL if the app is missing. Use `<queries>` entries, never `QUERY_ALL_PACKAGES`.
- **Settings** are a typed, versioned model with migrations, JSON import/export, and sensible defaults.

## 5. Performance rules (the app must be light)

- The wallpaper draws **only** on: visibility change to visible, data change, settings change, date/time/timezone change. **No timers, no continuous animation, no foreground service.** At most one short fade on data change, and it must be optional.
- Keep at most one screen-sized bitmap cache. Release it when the wallpaper becomes invisible. Reuse paints, layouts and fonts; no allocation in draw paths.
- Data refresh: `ContentObserver` on the Calendar Provider, `WorkManager` for periodic network sync (respect the 15-minute minimum), broadcasts that are exempt from implicit-broadcast limits (`ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED`, `ACTION_LOCALE_CHANGED`, `ACTION_BOOT_COMPLETED`), else register dynamically (`ACTION_DATE_CHANGED` is **not** exempt; ADR 0004).
- Bundle at most 3–4 OFL-licensed fonts as subsetted variable fonts, plus "system default". Keep APK small (target well under 15 MB; report the actual size in each release).
- Add a Baseline Profile for the app module. Add a `docs/performance.md` with measured numbers (idle PSS of the wallpaper process, APK size, draw time). Measure; do not guess. Treat regressions as bugs.

## 6. Customization (settings requirements)

Colors (palette presets, custom, optional Material You dynamic color), font family, text size scale, text and panel opacity, corner radius, spacing density, layout preset (Agenda list, Week strip, Month grid, Next-3 minimal), position and safe-zone insets, source and calendar/list selection, date range, week start, 12/24h, show/hide completed, privacy mode, battery mode (live vs static). Every setting must show in the **live preview** (rendered by the same Scene pipeline) and be covered by a settings-model test.

## 7. Security and privacy

- Todoist: user pastes a personal API token. (OAuth needs a client secret, which requires a backend; out of scope.) Store it encrypted with an Android Keystore AES-GCM key (or Tink) in DataStore; do not use deprecated `security-crypto`. Never log tokens, task titles or event titles. Exclude secrets from backups.
- HTTPS only (network security config). No analytics, ads, crash-reporting SDKs or accounts. Data stays on-device; the only network calls are to services the user enables.
- Permissions: `READ_CALENDAR` (requested in context, with a rationale screen), `INTERNET`, and for battery saver mode `SET_WALLPAPER` and `RECEIVE_BOOT_COMPLETED` (ADR 0004). WorkManager (via Glance and Todoist sync) also merges its normal permissions `WAKE_LOCK`, `ACCESS_NETWORK_STATE` and `FOREGROUND_SERVICE` (ADR 0007). Nothing else without an ADR.

## 8. Code quality

- **Every public class/function has KDoc** (what, why, threading, edge cases). Each module has a `README.md` (purpose, public API, dependencies rules). Non-obvious decisions get an ADR in `docs/adr/` (short: context, decision, consequences).
- Modular: dependencies point inward (`app` → `domain`/`render`/`data` → `core`). No module depends on `app`, except the test-only `baselineprofile` generator (ADR 0008). Enforce with a Gradle check.
- Small files and functions, no god classes, no `Util` dumping grounds. Prefer immutable models and pure functions in `render`/`domain`.
- Formatting and lint: ktlint (via Spotless) + detekt + Android Lint, all enforced in CI.
- Tests: JVM unit tests for layout engine, mappers, settings migrations; JVM screenshot tests for the renderer (Roborazzi or Paparazzi, so no emulator is needed); a few instrumented tests only where unavoidable.
- Conventional Commits. Small, reviewable commits. Work on feature branches and open PRs; CI must be green before merge.

## 9. Build, CI/CD, release (no local builds)

**CI is the compiler, and the owner has no local setup.** The owner works only through Claude Code on the web, the GitHub web UI and one Android phone: no Android Studio, no local Gradle, no adb. Do not assume the Claude Code sandbox can run Gradle or the Android SDK (its network access may block the Google/Gradle repositories); try it only if it plainly works, and never depend on it.

Feedback loop after any non-trivial change:
1. Push a branch and open a PR.
2. Read the CI result with whatever GitHub tooling this session has (checks/status, workflow run logs, PR comments; `gh` only if present and authenticated).
3. If you cannot read the logs, tell me exactly where to look and ask me to paste the failing step's output. Do not guess at fixes blindly.
4. To make this loop work, `ci.yml` must, on failure, post (or update) a single PR comment containing the failing step name and the last ~150 lines of its log, plus a short job summary. Use `permissions: pull-requests: write` only for that step and keep it fork-safe.

Never claim something builds or works until CI is green. Say plainly what you could not verify (e.g. on-device behavior).

Testing happens on one Android phone: the owner installs the APK from the PR's CI artifact or the GitHub Release page. Give short, phone-only test steps (no adb, no desktop tools) and ask for specific observations or screenshots in return. For performance numbers, prefer measurements CI can produce; otherwise give phone-only steps (Developer options) and say they are manual.

Workflows (`.github/workflows/`), least-privilege `permissions:`, actions pinned to a major version or SHA, Gradle caching:
- `ci.yml` (PR + main): spotless, detekt, lint, unit + screenshot tests, `assembleDebug`, upload debug APK artifact. Debug APKs are signed with a fixed debug-only key from the `DEBUG_KEYSTORE_BASE64` secret, passed only to the assemble step, so builds install over each other (ADR 0006). Never the release key.
- `release.yml` (tag `v*.*.*`): run checks, build **signed** release APK and AAB from secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), derive `versionName` from the tag and a monotonic `versionCode`, generate release notes from Conventional Commits, publish a **GitHub Release** with APK + AAB + SHA-256 checksums. The workflow must also work when the owner has already created the Release/tag from the GitHub web UI (upload assets to the existing release instead of failing).
- `pages.yml` (push to main under `site/`, plus manual dispatch): build and deploy `site/` via `actions/deploy-pages`. The download button reads the latest release client-side from the GitHub API, so the site never needs a rebuild per release. No release trigger: release runs execute on the tag ref, which the `github-pages` environment's protection rules reject.
- `play.yml` (manual dispatch, off until the repository variable `PLAY_UPLOAD_ENABLED` is `true`, after the first manual Play upload): upload the AAB of an existing GitHub Release to a Play track using a service account (`PLAY_SERVICE_ACCOUNT_JSON`, `google-play` environment). See `docs/play-store.md`.
- `baseline-profile.yml` (manual dispatch, or `[baseline-profile]` in a commit message on a non-main branch): generate the Baseline Profile on an emulator and commit it (ADR 0008). `screenshots.yml` does the same for screenshot goldens (`[record-screenshots]`).
- Dependabot or Renovate for dependencies and actions.
- Never commit keystores, tokens or `local.properties`. Keep `.gitignore` strict.

## 10. Play Store readiness (keep in mind from day one)

Final `applicationId` is permanent after publishing; do not change it. Ship AAB, current target SDK, 64-bit, adaptive + themed icon, `uses-feature android.software.live_wallpaper`, accessible text (font scaling, contrast warnings in settings), strings in `strings.xml` (localization-ready). Keep `fastlane/metadata/android/` (title, short/full description, changelogs, screenshots) in the repo. Privacy policy page lives at `site/privacy/` and states: calendar read access is local, no data collected or sold, Todoist token stays on-device. Prepare the Data safety and permission-declaration answers in `docs/play-store.md`.

## 11. Out of scope (do not build)

A full in-app task/calendar editor (we display; the source apps edit; keep only a minimal local list and quick-add), cloud backend, user accounts, ads, analytics, social or sharing features, iOS code (only keep `shared` modules iOS-compatible: no `java.*` or Android APIs in `core`, `domain`, `render`).

## 12. Working agreement

1. For anything non-trivial: plan first (short plan, list of files, risks), then implement in small steps.
2. Verify versions and APIs from current official docs; do not rely on memory.
3. Ask me before: changing the `applicationId`, adding permissions, adding a dependency with a network or tracking component, or changing the architecture in section 4.
4. When you finish a task: summarize in a few lines, list what is unverified, and update docs/ADRs.
