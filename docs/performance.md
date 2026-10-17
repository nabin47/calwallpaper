# Performance

Budgets from CLAUDE.md §5. Numbers must be measured, never estimated. CI numbers come first; the rest are
measured on the owner's phone with the steps below and recorded here with the phone model.

| Metric | Budget | Measured | How | Version |
|---|---|---|---|---|
| Release APK size | ≪ 15 MB | 0.76 MB (794,141 B) | Release asset size | v0.0.1 |
| Release AAB size | — | 1.71 MB (1,795,152 B) | Release asset size | v0.0.1 |
| Release APK size (unsigned, R8) | ≪ 15 MB | 1.38 MB (1,451,689 B) | CI "Release build size" step, run on 1f74955 | Phase 3 PR |
| Debug APK size | — (not shipped) | 12.4 MB (12,975,956 B) | CI job log, same run | Phase 3 PR |
| Release APK size (unsigned, R8) | ≪ 15 MB | 1.59 MB (1,662,644 B); +211 KB for Glance and the Compose runtime it brings | CI "Release build size" step, run on 192ae34 | Phase 4 PR |
| Debug APK size | — (not shipped) | 14.5 MB (15,240,866 B) | CI job log, same run | Phase 4 PR |
| Wallpaper layout time | < 16 ms | _not yet measured on a phone_ | App → Performance (below) | — |
| Wallpaper draw time (bitmap repaint) | < 16 ms | _not yet measured on a phone_ | App → Performance | — |
| Wallpaper bitmap memory | 1 screen-sized bitmap, freed when invisible | _not yet measured on a phone_ | App → Performance | — |
| Wallpaper process idle PSS | as low as possible | _not yet measured on a phone_ | App → Performance, cross-check in Developer options | — |

## What CI measures
Every CI run builds a debug APK and an unsigned, R8-optimized release APK. Both sizes are printed in the run's
**Summary** tab. The signed release sizes come from the GitHub Release assets.

## What the phone measures (no adb, no computer)
The live wallpaper measures itself and the app shows the numbers. It does not guess, and it stores no user content.

- **Layout:** time to turn the agenda into a Scene for the last frame.
- **Draw:** time to repaint the bitmap. This is `0` when nothing changed and the cached bitmap was reused.
- **Bitmap:** memory of the one frame bitmap. It is released while the wallpaper is invisible.
- **Process:** PSS of the `:wallpaper` process when the wallpaper last became invisible, measured at most every 10 minutes.

Steps:
1. Install the APK from the PR's CI artifact (`calwallpaper-debug-apk`) or from the release.
2. Open CalWallpaper, choose **Live**, tap **Set live wallpaper** and apply it.
3. Go to the home screen. Leave it for a few seconds, open and close an app a few times, and lock and unlock the phone once.
4. Open CalWallpaper and scroll to **Performance**. Write down the **Live wallpaper** numbers, then wait about 10 minutes
   and repeat to get a settled PSS value.
5. For battery saver: choose **Battery saver**, tap **Apply now**, and read the **Battery saver image** numbers.

Cross-check memory in **Developer options → Running services** (*Settings → System → Developer options*; enable them
by tapping *Build number* seven times in *About phone*). Find **CalWallpaper** and note the memory shown for the
`:wallpaper` process. Developer options show RAM in a different way from PSS, so record both, labelled.

Debug builds are larger and slower than release builds. Measure timing on a release build when possible
(GitHub Release page), and note which build you used.
