# Contributing

CI is the compiler: there is no assumption of a local Android toolchain. Every change goes through a PR.

## Workflow
1. Branch from `main` (`feat/...`, `fix/...`, `docs/...`).
2. Use [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `build:`, `ci:`, `refactor:`, `test:`, `chore:`; `!` for breaking). Release notes are generated from them, and squash-merge PR titles must follow the same format.
3. Open a PR. CI runs Spotless (ktlint), detekt, Android Lint, unit tests, and builds a debug APK.
   If it fails, a bot comment on the PR shows the failing step and the last 150 log lines.
4. Merge only when CI is green.

## Screenshot tests
CI compares every wallpaper layout against golden images (`android-wallpaper/src/test/screenshots/`).
If you change how something looks on purpose, CI fails at **Screenshot tests**. The `build-reports`
artifact of that run has the expected, actual and diff images. To accept the new look:

1. Open **Actions → Record screenshots → Run workflow**, and pick your PR branch (never `main`).
2. The workflow records the new goldens, commits them to the branch, and starts CI again.
3. Review the changed PNGs in the PR's "Files changed" tab.

Alternatively, include `[record-screenshots]` in a commit message on the branch.

## Local commands (if you do have a toolchain)
```
./gradlew spotlessApply      # format
./gradlew spotlessCheck detekt lint check :app:assembleDebug
```

## Rules
- Every public class/function has KDoc (detekt enforces it). Each module keeps its `README.md` current.
- Dependencies point inward; `build-logic/.../ModuleGraph.kt` fails the build otherwise.
- Warnings are errors (Kotlin and Lint). Fix them, do not suppress them.
- Non-obvious decisions get an ADR in `docs/adr/`.
- Never commit keystores, tokens, or `local.properties`.
