# baselineprofile

Generates the app's **Baseline Profile** (CLAUDE.md §5, ADR 0008): the list of classes and methods that run on
the app's hot paths, which Android compiles ahead of time at install. It is a test-only `com.android.test`
module; nothing from it ships in the app.

## What it records
`BaselineProfileGenerator` drives the release app (non-minified) on an emulator:
- app start, the first-run steps and scrolling the settings with the live preview;
- the live wallpaper in the system's preview, which runs the wallpaper process's layout and Canvas renderer.

Calendar access is never granted, so the preview uses the sample agenda. No accounts or real data are involved.

## How to regenerate (no local setup needed)
**Actions → Baseline Profile → Run workflow**, on your PR branch (never `main`). Or push a commit whose
message contains `[baseline-profile]`. The workflow runs `./gradlew :app:generateBaselineProfile` on a
Gradle Managed Device (Pixel 6, API 34, AOSP image), commits the result to
`app/src/release/generated/baselineProfiles/` and starts CI on that commit.

Regenerate after large changes to startup, the settings screen or the wallpaper's draw path.

## Dependency rules
- Depends only on `:app` (through `targetProjectPath`), and `:app` reads the generated profile from it. This is
  the one module allowed to reference `:app` (`build-logic` `ModuleGraph`).
- Library code: Macrobenchmark, UiAutomator and AndroidX Test JUnit only.
