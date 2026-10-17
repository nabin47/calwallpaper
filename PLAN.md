# PLAN — Phase 0 scaffold (draft for approval)

Status: **Phase 0 complete 2026-10-17** (v0.0.1 released). Approved 2026-10-16. Decisions: name CalWallpaper, applicationId `io.github.nabin47.calwallpaper`, Apache-2.0, Room 3, manual DI, detekt 2.0 alpha pinned (ADR 0001). Versions verified on 2026-10-16 (Maven Central metadata + official release pages; Google Maven is not reachable from the sandbox, so Google artifacts were checked on developer.android.com).

## 1. Module graph (dependencies point inward)

```
app ──────────────┬─► android-wallpaper ─┐
  │               └─► android-widgets ───┤
  ├─► data ──────────────────────────────┼─► domain ─► core
  └─► render ◄───────────────────────────┘     render ─► domain, core
```
- `core`, `domain`, `render`: KMP, `jvm()` target only for now (pure Kotlin, JVM unit tests; Android consumes the JVM variant). iOS targets are added later; the code stays iOS-safe (no `java.*`).
- `data`: KMP with the Android target (`com.android.kotlin.multiplatform.library`, `kotlin { android { } }` DSL).
- `android-wallpaper`: Android library (no Compose, no DI). `android-widgets`: Android library + Glance. `app`: Android application, Compose.
- A `build-logic` check fails the build if any module depends on `:app`, or if `core/domain/render` depend on an Android module.

## 2. Folder layout
`build-logic/convention` (plugins: `calwp.kmp.jvm`, `calwp.kmp.android`, `calwp.android.library`, `calwp.android.app`, `calwp.compose`, `calwp.quality`), `gradle/libs.versions.toml`, the modules above, `site/` (+ `site/privacy/`), `docs/` (`architecture.md`, `adr/0001-stack.md`, `release.md`, `performance.md` stub, `play-store.md` stub), `fastlane/metadata/android/en-US/`, `.github/workflows/{ci,release,pages,play}.yml`, `.github/dependabot.yml`.

## 3. Versions (latest stable as of 2026-10-16)

| Item | Version | Source |
|---|---|---|
| Gradle | 9.8.1 | [services.gradle.org/versions/current](https://services.gradle.org/versions/current) |
| AGP | 9.4.0 (needs Gradle ≥ 9.6.0, JDK 17, max API 37) | [AGP 9.4.0 notes](https://developer.android.com/build/releases/agp-9-4-0-release-notes) |
| Kotlin | 2.4.21 | [kotlinlang releases](https://kotlinlang.org/docs/releases.html) |
| KSP | 2.3.12 (KSP2, decoupled from Kotlin versions) | Maven Central `symbol-processing-gradle-plugin` |
| Compose BOM | 2026.09.00 (Compose 1.12.1, M3 1.4.0) | [BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) |
| Glance | 1.2.0 | [Glance releases](https://developer.android.com/jetpack/androidx/releases/glance) |
| Room | **3.0.3** (`androidx.room3`) — 2.8.5 is the 2.x line | [Room 3](https://developer.android.com/jetpack/androidx/releases/room3), [Room](https://developer.android.com/jetpack/androidx/releases/room) |
| DataStore | 1.2.1 (1.3.0 is RC only) | [DataStore releases](https://developer.android.com/jetpack/androidx/releases/datastore) |
| WorkManager | 2.12.0 (minSdk 24) | [Work releases](https://developer.android.com/jetpack/androidx/releases/work) |
| Ktor | 3.6.0 | Maven Central `ktor-bom`, [Ktor 3.6.0 blog](https://blog.jetbrains.com/zh-hans/ktor/2026/09/20/ktor-3-6-0-is-now-available/) |
| Koin | 4.2.2 (proposed: **not used**, see §5) | Maven Central `koin-core` |
| coroutines / serialization / datetime | 1.11.0 / 1.11.0 / 0.8.0 | Maven Central |
| Spotless (+ ktlint) | 8.10.4 | Maven Central |
| detekt | 1.23.x stable does not support Kotlin 2.4; only `dev.detekt` **2.0.0-alpha.6** does | [detekt 2.0 changelog](https://detekt.dev/changelog-2.0.0/) |
| Roborazzi | 1.76.0 (later phase) | Maven Central |
| Play target SDK | **API 36 required** for new apps/updates since 2026-08-31 | [Play target SDK](https://developer.android.com/google/play/requirements/target-sdk) |

`compileSdk`/`targetSdk`: **37** (Android 17, supported by AGP 9.4). This is at or above the Play minimum of 36. It falls back to 36 if the API 37 platform isn't on the CI runner image.

## 4. AGP 9 + KMP setup (current, non-deprecated)
- AGP 9 has built-in Kotlin, so `org.jetbrains.kotlin.android` is **not** applied to Android modules.
- KMP modules with an Android target use `com.android.kotlin.multiplatform.library` with `kotlin { android { namespace; compileSdk; minSdk; withHostTest {} } }`. `androidLibrary {}` is deprecated since 9.1, and `com.android.library` cannot be combined with KMP. ([plugin docs](https://developer.android.com/kotlin/multiplatform/plugin), [KMP AGP 9 migration](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html))
- Compose: `org.jetbrains.kotlin.plugin.compose`. R8: AGP 9.3+ `optimization { enable = true }` (full mode is already the default; this one switch covers code and resource shrinking). Keep rules go in `src/main/keepRules/*.keep`.

## 5. CLAUDE.md §3 review — proposed changes
1. **Room → Room 3 (`androidx.room3`)**. It is stable, KMP-first, KSP-only and driver-based, which fits the CLAUDE.md rules. Room 2.x is now the legacy line. *Needs your OK (CLAUDE.md wording change).*
2. **DI → manual constructor injection** (ADR 0001). It needs no library or reflection and adds no wallpaper-path cost. Koin isn't needed at this size.
3. **detekt**: the only Kotlin-2.4-compatible release is an alpha. Options: (a) use `dev.detekt` 2.0.0-alpha.6 now and pin it (my recommendation), or (b) leave detekt out until 2.0 is stable. Either way it conflicts with "never use pre-release". *Your call.*
4. Nothing else in §3 is deprecated. Glance, DataStore, Ktor (OkHttp engine), WorkManager, kotlinx.* and Spotless/ktlint are all current. `security-crypto` is already banned; use Tink or a Keystore AES-GCM key in a later phase.

## 6. Phases
0. **Scaffold + CI/CD**: done (v0.0.1).
1. **Data foundation**: done. core models (`AgendaItem`, `OpenTarget`, time utils), `AgendaSource`,
   `LocalTasksSource` (Room 3), `CalendarProviderSource`, and versioned settings with DataStore, migrations and
   JSON import/export. See ADR 0002.
2. **Render**: done. The Scene and the layout engine with all four presets, privacy and safe zones; the Android
   Canvas renderer and Roborazzi screenshot tests. See ADR 0003.
3. **Wallpaper + app** (current): the WallpaperService in its own process with event-driven redraws, one cached
   bitmap, the Direct Boot cache and lock-state privacy; static battery mode; onboarding with the calendar
   rationale; settings for every §6 option with a live preview; the set-as-wallpaper flow; on-device performance
   numbers. See ADR 0004.
4. Local task list UI (minimal list and quick-add), with cross-process invalidation for the wallpaper.
5. Glance widgets and deep links (`OpenTarget`).
6. Todoist (encrypted token) and WorkManager sync.
7. Remaining presets and customization, Baseline Profile, performance doc, Play readiness.

## 7. Risks
- **CI-only builds:** the sandbox can't reach Google Maven, so every Gradle error costs a CI round-trip. Mitigation: small pushes, and `ci.yml` posts failure logs to the PR.
- **Gradle wrapper jar:** I can't run `gradle wrapper` here. I'll commit `gradlew` scripts plus `gradle-wrapper.properties` with `distributionSha256Sum`, and fetch the official wrapper jar from services.gradle.org. CI uses `gradle/actions/wrapper-validation`.
- **KSP 2.3.12 × Kotlin 2.4.21** isn't explicitly documented. Phase 0 doesn't need KSP, so this is deferred to Phase 3.
- Live-wallpaper tap delivery varies by launcher (§2), so it has to be verified on your phone.
- `versionCode` derivation: I propose `major*1_000_000 + minor*1_000 + patch`. It is monotonic for semver tags and lets you rebuild a tag.

## 8. Owner actions
- Enable GitHub Pages once: *Settings → Pages → Source: GitHub Actions*.
- After merging, create release `v0.0.1` (docs/release.md).
