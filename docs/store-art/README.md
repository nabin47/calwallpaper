# Store art sources

Sources of the Play Store images in `fastlane/metadata/android/en-US/images/`. Both reuse the launcher icon's
vector paths (`app/src/main/res/drawable/ic_launcher_foreground.xml`) and its color `#1F3A5F`.

| File | Output | Play requirement |
|---|---|---|
| `icon.html` | `images/icon.png` | 512 × 512, 32-bit PNG, full square (Play applies the rounded mask) |
| `feature.html` | `images/featureGraphic.png` | 1024 × 500, 24-bit PNG or JPEG |

The text uses Inter (OFL). To re-render with headless Chromium:

```sh
chrome --headless --hide-scrollbars --force-device-scale-factor=1 --window-size=512,512 --screenshot=icon.png icon.html
chrome --headless --hide-scrollbars --force-device-scale-factor=1 --window-size=1024,500 --screenshot=featureGraphic.png feature.html
```

then convert `icon.png` to RGBA and `featureGraphic.png` to RGB (for example with Pillow).

Phone screenshots (`images/phoneScreenshots/`, at least 2, up to 8) are taken by hand on a phone; see the
checklist in `docs/play-store.md`.
