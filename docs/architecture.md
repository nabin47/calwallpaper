# Architecture

See CLAUDE.md §4 for the authoritative module list and key ideas. This page tracks how it is built.

## Modules

| Module | Kind | Depends on | Purpose |
|---|---|---|---|
| `core` | KMP (`jvm`) | — | `AgendaItem`, `OpenTarget`, `SourceId`, time utils, `Outcome`. No Android, no `java.*`. |
| `domain` | KMP (`jvm`) | core | `AgendaSource`, versioned `Settings` + `SettingsCodec` + migrations, `SettingsRepository`. |
| `render` | KMP (`jvm`) | core, domain | Layout engine → platform-neutral `Scene`. |
| `data` | KMP (Android target) | core, domain | `CalendarProviderSource`, `LocalTasksSource` (Room 3), DataStore settings; later Ktor/Todoist. |
| `android-wallpaper` | Android library | core, domain, render, data | `WallpaperService` + Canvas renderer. No Compose, no DI framework. |
| `android-widgets` | Android library | core, domain, render, data | Glance widgets. |
| `app` | Android application | all of the above | Settings UI (Compose), onboarding, permissions, deep-link router. |

The rules are enforced by `build-logic/convention/src/main/kotlin/ModuleGraph.kt`, which fails the
build on any edge not in the table (and on any dependency on `:app`).

KMP modules target only the JVM for now. iOS targets get added in the iOS phase; until then,
`commonMain` code must stay free of `java.*` and Android APIs.

## Build logic
Convention plugins live in `build-logic/convention` (an included build):

| Plugin id | Applies |
|---|---|
| `calwp.root` | Spotless for build scripts and build-logic; module-graph check |
| `calwp.kmp.jvm` | Kotlin Multiplatform, `jvm()` target, `kotlin("test")`, quality gates |
| `calwp.kmp.android` | Kotlin Multiplatform + `com.android.kotlin.multiplatform.library`, quality gates |
| `calwp.android.library` | `com.android.library` (built-in Kotlin), SDK levels, strict lint, quality gates |
| `calwp.android.application` | `com.android.application` (built-in Kotlin), SDK levels, strict lint, quality gates |
| `calwp.android.compose` | Compose compiler plugin + Compose BOM (never on `android-wallpaper`) |

The quality gates are Spotless (ktlint), detekt (`config/detekt/detekt.yml`), and Kotlin `allWarningsAsErrors`.
Android Lint uses `lint.xml` with warnings as errors.

Versions live only in `gradle/libs.versions.toml`.

## Release shrinking
`app` release uses AGP's `optimization { enable = true }`, which turns on R8 full mode plus resource
shrinking. Keep rules, when needed, go in `app/src/main/keepRules/*.keep`.
