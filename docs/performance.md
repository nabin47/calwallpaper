# Performance

Budgets from CLAUDE.md §5. Numbers must be measured, never estimated. CI-produced numbers come first.

| Metric | Budget | Measured | How | Release |
|---|---|---|---|---|
| Release APK size | ≪ 15 MB | 0.76 MB (794,141 B) | Release asset size | v0.0.1 |
| Release AAB size | — | 1.71 MB (1,795,152 B) | Release asset size | v0.0.1 |
| Wallpaper process idle PSS | as low as possible | _not yet (Phase 2)_ | Manual: Developer options → Running services | — |
| Scene build + draw time | < 16 ms | _not yet (Phase 2)_ | JVM benchmark / trace | — |
