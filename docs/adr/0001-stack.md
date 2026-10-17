# ADR 0001: Technology stack

- Status: accepted
- Date: 2026-10-16

## Context
CalWallpaper must be light (the wallpaper process runs all the time), Android-first, iOS-ready for shared
logic, and buildable only in CI (the owner has no local toolchain). CLAUDE.md §3 fixed most of the stack.
This ADR records the choices that §3 left open, plus the versions checked at scaffold time.

## Decision
- **Kotlin Multiplatform** for `core`, `domain`, `render`, `data`; native Android for wallpaper, widgets and app.
  Gradle 9.8.1, AGP 9.4.0 (built-in Kotlin; the KMP Android target uses `com.android.kotlin.multiplatform.library`
  with the `kotlin { android { } }` DSL), Kotlin 2.4.21. compileSdk/targetSdk 37, minSdk 26.
- **DI: manual constructor injection, no Koin.** The object graph is small, and manual wiring adds no
  library, no reflection, and no startup cost in the wallpaper process. Revisit only if wiring code gets painful.
- **Persistence: Room 3 (`androidx.room3`)** instead of Room 2.x. Room 3 is stable, KMP-first, KSP-only and
  coroutine-only, built on `SQLiteDriver`. Starting a new project on the legacy 2.x line would mean a migration
  later. DataStore stays for settings.
- **detekt 2.0.0-alpha.6 (`dev.detekt`), pinned.** detekt 1.23.x (the latest stable) does not support Kotlin 2.4;
  only the 2.0 alphas do. This is a deliberate, bounded exception to the "no pre-release" rule: detekt is a
  build-time linter that never ships in the APK. Move to 2.0.0 stable as soon as it is released.
- **Tests:** `kotlin.test` on JUnit 4. Robolectric/Roborazzi screenshot tests (later phases) need JUnit 4 runners.
- **CI caching:** `gradle/actions/setup-gradle` with `cache-provider: basic` (MIT-licensed), which avoids the
  proprietary default cache provider's terms.

## Consequences
- detekt 2.0.0-alpha.6 calls the deprecated `Configuration.setVisible` (it will be removed in Gradle 11). This is the
  only Gradle deprecation in the build (`org.gradle.warning.mode=all` lists it). It comes from upstream and
  cannot be fixed here; check it again on each detekt upgrade.
- Detekt rule or config names may change between alphas. Pin the exact version and read its changelog on upgrade.
- Room 3 requires KSP. KSP 2.3.x is decoupled from Kotlin versions, but its compatibility with Kotlin 2.4 must
  be confirmed by CI when `data` adds Room (Phase 3).
- With no DI container, `app` owns the composition root; `android-wallpaper` builds its own small graph.
