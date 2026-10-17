# Releasing

Releases are built only by GitHub Actions (`.github/workflows/release.yml`). They need these repository
secrets (Settings → Secrets and variables → Actions):

| Secret | Meaning |
|---|---|
| `KEYSTORE_BASE64` | Upload/release keystore, base64-encoded |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_PASSWORD` | Key password |

`.github/scripts/decode-keystore.sh` checks the secrets before every build. Releases fail with a clear message
when something is wrong. PR CI never receives these secrets, because it runs code from the PR.

### If the secrets check fails
- Names must match exactly (upper case). They must be **Repository secrets** under *Actions*. Secrets saved
  under *Environments*, *Dependabot* or *Codespaces* are not visible to these workflows.
- `KEYSTORE_BASE64` must be the base64 of the binary `.jks`/`.keystore` file, e.g. `base64 -w0 release.jks`
  (Linux) or `base64 -i release.jks` (macOS). Line breaks are tolerated. A file path, a PEM certificate,
  or the password is not valid.
- `KEYSTORE_PASSWORD` must open the keystore, and `KEY_ALIAS` must exist in it.

## Version numbers
- `versionName` = the tag without `v` (`v1.2.3` → `1.2.3`).
- `versionCode` = `major × 1,000,000 + minor × 1,000 + patch` (`v1.2.3` → `1002003`). It increases with every
  higher tag and is reproducible when a tag is rebuilt. Minor and patch must be ≤ 999.
- Builds outside a release are `0.0.0-dev` (code 1).

## Option A: from the GitHub web UI (no tools needed)
1. Merge the PR into `main` and wait for CI on `main` to be green.
2. Go to **Releases → Draft a new release**, type a new tag such as `v0.0.2`, target `main`.
3. Optionally leave the description empty; it is then filled from the Conventional Commits.
4. Click **Publish release**. The *Release* workflow builds and attaches:
   `CalWallpaper-vX.Y.Z.apk`, `CalWallpaper-vX.Y.Z.aab`, `SHA256SUMS.txt`.

## Option B: push a tag
`git tag v0.0.2 && git push origin v0.0.2`. The workflow creates the Release itself.

If both a tag push and a release event fire for the same tag, the second run sees the assets and skips.
To rebuild an existing tag, run the *Release* workflow manually (**Actions → Release → Run workflow**) with that tag.

## What the workflow checks
Spotless, detekt, lint, unit tests, then builds the signed APK + AAB and verifies the APK signature with
`apksigner`. The R8 `mapping.txt` is stored as a private workflow artifact for 90 days.

## Installing on the phone
Open the release page on the phone, download the `.apk`, and allow "Install unknown apps" for the browser
when asked. Debug builds from PR CI install side by side, as **CalWallpaper Debug** (`….debug`).

## Debug APK signing
CI signs debug APKs with one fixed, **debug-only** key, so each new "CalWallpaper Debug" installs over the
previous one (ADR 0006). The key is the repository secret `DEBUG_KEYSTORE_BASE64`. It is a base64-encoded PKCS12
keystore with alias `androiddebugkey` and the standard debug password `android`. It is never the release key,
and only the "Assemble debug APK" step receives it.

- **Without the secret** (fork PRs, or before it is added), the APK uses the runner's throwaway debug key. The step
  warns about it, and installing that APK over another needs an uninstall first.
- **When switching keys** (the first fixed-key build after throwaway ones, or after replacing the key), uninstall
  "CalWallpaper Debug" once.
- **Replacing the key:** generate a new PKCS12 keystore with the same alias and password, then update the secret.

Release APKs always use the release key and update in place.

## Release size
The job summary of each release run lists the APK and AAB sizes. Record them in `docs/performance.md`.
