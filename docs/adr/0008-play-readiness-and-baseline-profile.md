# ADR 0008: Play readiness, Baseline Profile and accessibility

- Status: accepted
- Date: 2026-10-17

## Context
Phase 6 prepares the first Google Play release (CLAUDE.md §10). It also adds the Baseline Profile that
CLAUDE.md §5 asks for. Constraints:

- **No local tools.** The owner has no Android Studio, emulator or adb, so everything must run in GitHub
  Actions. That includes generating the profile, which needs a device.
- **First upload by hand.** Play requires the first AAB to be uploaded by hand in the Play Console. Automated
  uploads need a service account that only the owner can create.
- **Module rules.** Nothing may depend on `:app` (CLAUDE.md §4, §8, enforced by `ModuleGraph`). AndroidX's
  standard Baseline Profile setup needs a `com.android.test` module that targets the app.

## Decision
- **Baseline Profile generator module.** `:baselineprofile` is a `com.android.test` module.
  - It uses androidx.baselineprofile and Macrobenchmark 1.5.0. 1.5.0 is the first stable release that needs no
    `newDsl=false` on AGP 9.
  - It drives the non-minified release app through start, onboarding and scrolling the settings with the live
    preview. It also opens the system's live-wallpaper preview, which runs the `:wallpaper` process's layout and
    renderer.
  - It runs on a Gradle Managed Device: Pixel 6, API 34, AOSP image. AOSP rather than Google, because the
    generator needs root on an emulator.
  - It is the one module allowed to reference `:app`, and `:app` reads the generated profile from it.
    `ModuleGraph` lists both edges. Nothing from it ships in the app.
- **Generated in CI, committed to the repo.** `baseline-profile.yml` runs `:app:generateBaselineProfile` on the
  runner's KVM. Start it by hand, or with `[baseline-profile]` in a commit message on a non-main branch. It
  commits `app/src/release/generated/baselineProfiles/` and starts CI on that commit, like `screenshots.yml` does
  for goldens. Release builds compile in whatever profile is committed. PR CI only compiles the generator.
- **ProfileInstaller** (1.4.1) is added, so APKs installed from GitHub Releases also get the profile; Play
  applies it on its own. Its initializer runs in the main process only. The wallpaper process is unaffected,
  because content providers start only in their declared process.
- **`play.yml`, ready but off.**
  - It uploads the AAB of an existing GitHub Release; it never rebuilds. It checks the AAB against
    `SHA256SUMS.txt` before uploading.
  - Release notes come from `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, or `default.txt`.
  - It adds the R8 mapping from the release run while that artifact still exists.
  - It uses `r0adkll/upload-google-play`, pinned by commit (v1.1.5).
  - The service-account key is the `PLAY_SERVICE_ACCOUNT_JSON` secret in a `google-play` environment, which can
    require approval per run.
  - Until the repository variable `PLAY_UPLOAD_ENABLED` is `true`, the job fails at once with instructions.
  - Listing text stays manual, so the Play Console remains the place where the listing is reviewed.
- **Data safety: "no data collected".**
  - Calendar data and local tasks never leave the device.
  - Todoist is only read. Its token is a credential for the user's own account, sent only to Todoist at the
    user's choice.
  - `docs/play-store.md` records the reasoning and the conservative fallback answer.
- **Accessibility pass.**
  - Wallpaper text follows Android 14+'s non-linear font scaling, sampled at the body size through
    `TypedValue.applyDimension`.
  - Day numbers and weekday initials shrink to fit their cells. Before this, the largest font size made
    neighboring numbers overlap (found by a new test).
  - The contrast warning checks every text color, not just the main text. The Paper palette's accent failed at
    4.4:1 and is now 5.1:1.
  - In settings, sections are headings, sliders announce their name and value, switch rows are one control, and
    swatches are 48 dp touch targets.

## Consequences
- **One more module.** It adds a build, and CI compiles the generator on every PR. The profile goes stale as the
  code changes. Regenerate it after large changes to startup, settings or the draw path; a stale profile only
  costs speed.
- **Generation is unverified until it runs.** It depends on the runner's emulator; if it fails, its log is in the
  workflow run. It cannot exercise the home screen with the wallpaper set, widgets or taps: those need user
  actions in system UI.
- **No startup numbers yet.** The real gain needs a Macrobenchmark on a physical device, which the owner does
  not have. `docs/performance.md` says so instead of guessing.
- **Two signing keys.** Play App Signing (Google-held key) means Play installs and GitHub Release APKs cannot
  update each other. This is documented for users in `docs/play-store.md`.
