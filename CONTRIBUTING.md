# Contributing

CI is the compiler: there is no assumption of a local Android toolchain. Every change goes through a PR.

## Workflow
1. Branch from `main` (`feat/...`, `fix/...`, `docs/...`).
2. Use [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `build:`, `ci:`, `refactor:`, `test:`, `chore:`; `!` for breaking). Release notes are generated from them, and squash-merge PR titles must follow the same format.
3. Open a PR. CI runs Spotless (ktlint), detekt, Android Lint, unit tests, and builds a debug APK.
   If it fails, a bot comment on the PR shows the failing step and the last 150 log lines.
4. Merge only when CI is green.

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
