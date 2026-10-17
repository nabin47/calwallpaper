# Testing taps and widgets on a phone

Wallpaper taps depend on the launcher (ADR 0005). Widgets do not. These steps need only the phone: no computer,
no adb.

## Which launchers to test
Tap delivery is the launcher's choice. Android sends `WallpaperManager.COMMAND_TAP` only when the launcher asks for
it, and only for taps on empty space (not on icons or widgets).

| Priority | Launcher | Why | Expectation |
|---|---|---|---|
| 1 | **Your phone's own launcher** | What you use every day | Unknown until tested |
| 2 | **Pixel Launcher** (Pixel phones) | Built on AOSP Launcher3, which sends taps | Taps should work |
| 3 | **One UI Home** (Samsung) | The largest Android launcher; closed source | Unknown: test |
| 4 | **Lawnchair** (free, Play Store or GitHub) | A Launcher3 fork you can install on any phone | Taps should work; the easiest second opinion |
| 5 | **Nova Launcher** (Play Store) | A popular third-party launcher; closed source | Unknown: test |
| 6 | Xiaomi (HyperOS), OnePlus/Oppo (ColorOS), Motorola | Large OEM shares; closed source | Unknown: test if you have one |

With one phone, test priority 1. Then install Lawnchair (priority 4) as a temporary home app, and test it.

To change the home app: **Settings → Apps → Default apps → Home app**. Switch back afterwards.

If a launcher does not send taps, the wallpaper still draws normally; only taps do nothing. Widgets work on every
launcher.

## Steps
Install the debug APK from the PR's checks page (artifact `calwallpaper-debug-apk`).

1. **Wallpaper taps.**
   1. Set CalWallpaper as the live wallpaper (app → **Set live wallpaper**). Choose home, or home and lock.
   2. On the home screen, find a free spot on top of the agenda panel, with no icon or widget above it.
   3. Tap an event row. Its event should open in your calendar app.
   4. Tap a day heading. Your calendar should open on that day.
   5. Tap empty wallpaper outside the panel. Nothing should happen.
2. **Lock screen.** Lock the phone and tap the agenda. Nothing should happen (Android never sends lock-screen taps).
3. **Widgets.**
   1. Long-press the home screen → **Widgets** → **CalWallpaper**.
   2. Add **Agenda** and **Week**.
   3. Tap an event in each. It should open in the calendar.
   4. Tap a day in **Week**. The calendar should open on that day.
   5. Tap an empty part of a widget. The CalWallpaper app should open.
4. **Widgets stay current.**
   1. Add or move an event in your calendar app. Within about a minute, both widgets should show it.
   2. Change the text size or colors in CalWallpaper. The widgets should follow a few seconds later.
5. **No calendar app.** Optional, only if your phone lets you disable it: disable the calendar app (Settings →
   Apps → Calendar → Disable), then tap an event. Google Calendar should open in the browser on that day. Re-enable
   the calendar app afterwards.

## Please report back
- Your phone model and launcher name.
- For each step, whether it worked. If a tap did nothing, say whether the screen flickered or simply nothing happened.
- A screenshot of each widget.
