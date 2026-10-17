# ADR 0006: Fixed debug signing key in CI

- Status: accepted
- Date: 2026-10-17

## Context
The owner installs every CI build on one phone. Each CI runner creates a new throwaway debug key, and Android
refuses to update an app with an APK signed by a different key. So every new debug build needed an uninstall
first, which also erased the app's settings. The owner asked for a fixed debug key.

Constraints:
- **No keystores in git** (CLAUDE.md §9).
- **PR CI runs PR code**, so the release key must never reach it.
- **The owner has no local tools**, so the key cannot be made on a computer of theirs.

## Decision
- **A debug-only key.** A dedicated PKCS12 keystore holds the key (alias `androiddebugkey`, standard debug password
  `android`). It is generated once and stored as the repository secret `DEBUG_KEYSTORE_BASE64`. It is not, and never
  becomes, the release key.
- **Narrow exposure.** Only the "Assemble debug APK" step receives the secret.
  - `.github/scripts/assemble-debug.sh` decodes it into the runner's temp directory and builds.
  - Gradle (`app/build.gradle.kts`) signs the debug build type with it whenever `DEBUG_KEYSTORE_PATH` is set.
- **Graceful fallback.** Without the secret (fork PRs, which GitHub gives no secrets, or before it is added), the
  build falls back to the throwaway key with a warning.

## Consequences
- **CI builds install over each other,** and app data survives updates.
- **The key is visible to that one step.** Code in a same-repository PR could read the debug key there. If it
  leaked, someone could sign an APK that installs over "CalWallpaper Debug" (`….debug`). That is a separate app
  from the release, and it is only sideloaded by the owner. That risk is accepted. The release key stays only in
  `release.yml`.
- **Key changes need one uninstall.** Moving to this key, or replacing it, requires removing "CalWallpaper Debug"
  once.
