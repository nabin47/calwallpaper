# Google Play readiness

Everything needed for the first Google Play release (CLAUDE.md §10): facts, prepared answers for every Play
Console declaration, the pre-launch checklist, and the steps only the owner can do in the Play Console. Answers
were checked against the code on 2026-10-17; re-check them when permissions, sources or libraries change.

## Facts
| | |
|---|---|
| App name | CalWallpaper |
| Package (`applicationId`) | `io.github.nabin47.calwallpaper` (permanent once uploaded) |
| Target / compile SDK | 37. Play requires ≥ 36 for new apps and updates since 2026-08-31. |
| Min SDK | 26 |
| Format | AAB from `release.yml`, signed with the upload key (`KEYSTORE_*` secrets) |
| Native code | None (CI's job summary lists any `.so` in the release APK), so the 16 KB page-size rule is met |
| Privacy policy | https://nabin47.github.io/calwallpaper/privacy/ (`site/privacy/index.html`) |
| Website | https://nabin47.github.io/calwallpaper/ |
| Listing text, changelogs, icon, feature graphic | `fastlane/metadata/android/en-US/` |
| First Play release | `v0.1.0`, versionCode 1000, notes in `changelogs/1000.txt` |

## Data safety (Play Console → App content → Data safety)
Play counts data as **collected** when the app sends it off the device, and as **shared** when it is passed to
a third party. Data that the app only processes on the device need not be declared. CalWallpaper has no server
and no SDKs that send data.

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| (The encryption-in-transit and deletion questions are only asked when the answer above is Yes.) | — |

Why "No", per data type the app touches:

| Data | What happens | Collected? |
|---|---|---|
| Calendar events (READ_CALENDAR) | Read on the device to draw the wallpaper and widgets; never sent | No: on-device only |
| Local tasks (quick-add) | Stored in a database on the device; never sent | No: on-device only |
| Todoist tasks and projects | Downloaded from Todoist into an on-device cache; never uploaded | No: nothing is sent off the device but the request |
| Todoist personal token | Sent only to `api.todoist.com` as the `Authorization` header of the user's own requests, at the user's choice | No listed data type; see note |
| Tap targets (event id and time, Todoist link) | Handed to the calendar app, Todoist app or browser on tap | No: a user-initiated action, the expected result of the tap |
| Settings, caches, diagnostics | On the device; diagnostics are only shown in settings | No |

**Note on Todoist.** The app calls Todoist's API only after the user pastes their own token, to read their own
tasks. It uploads no user content, and a token is not one of Play's data types. If Google ever asks, the
conservative alternative is to declare *Personal info → User IDs* as collected (not shared), *processed
ephemerally: no*, *required: no (optional)*, purpose *App functionality*, encrypted in transit *Yes*, deletion
*Yes* (disconnect in the app). Do not use it unless asked; it would show "Data collected" on the listing.

## Permission declarations
None of the app's permissions is on Play's restricted list, so no permission declaration form is needed.

| Permission | Kind | Why (shown to Play reviewers if asked) |
|---|---|---|
| `READ_CALENDAR` | runtime | Core feature: shows the user's events on the wallpaper and widgets. Asked in context, after a rationale screen, never at startup. Read-only. |
| `INTERNET` | normal | Only for Todoist, when the user connects it. HTTPS only (network security config). |
| `SET_WALLPAPER` | normal | Battery saver mode sets a still image of the agenda as the wallpaper (ADR 0004). |
| `RECEIVE_BOOT_COMPLETED` | normal | Battery saver mode and widgets re-arm their update triggers after a reboot. The receiver is disabled while neither is in use. |
| `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` | normal | Merged from WorkManager (widgets, Todoist sync). The app runs no foreground service and declares **no foreground-service type**, so Play's foreground-service declaration does not apply (ADR 0007). |
| `<appId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | signature | Added by AndroidX Core; only this app holds it. |

- Not used: `QUERY_ALL_PACKAGES` (narrow `<queries>` instead; ADR 0005), exact alarms, `AD_ID`, location,
  photos/media, accessibility services, full-screen intents.
- CI's job summary lists the permissions of the release APK on every PR. If a new one appears, update this
  table, CLAUDE.md §7 and the privacy policy before releasing.

## Other App content declarations
| Declaration | Answer |
|---|---|
| Privacy policy | https://nabin47.github.io/calwallpaper/privacy/ |
| Ads | **No**, the app contains no ads |
| App access | **All or some functionality is restricted**. Instructions: "All features work without an account. The optional Todoist source (Settings → Todoist) needs a Todoist personal API token: paste the token below." Then add a token from a throwaway Todoist account made for review (manual step 7 below). |
| Content rating (IARC) | Category: *All other app types* (utility). Every content question: **No**. User interaction or content sharing: **No**. Shares location: **No**. Purchases: **No**. Expected rating: Everyone / PEGI 3. |
| Target audience | **13–15, 16–17, 18 and over**. Not designed for children; "appeals to children": **No**. Do not select ages under 13, which brings in the Families policy. |
| News app | No |
| Government app | No |
| Financial features | None |
| Health | None |
| Advertising ID | **No**, the app does not use it (no `AD_ID` permission) |
| Data safety | See above |

## Store listing
- **Title** `title.txt` (≤ 30 chars), **short description** `short_description.txt` (≤ 80), **full description**
  `full_description.txt` (≤ 4000): copy from `fastlane/metadata/android/en-US/`.
- **App icon**: `images/icon.png` (512 × 512). **Feature graphic**: `images/featureGraphic.png` (1024 × 500).
  Sources in `docs/store-art/`.
- **Phone screenshots**: at least 2, up to 8, taken on the phone (manual step 4). Save them to
  `images/phoneScreenshots/` (`1.png`, `2.png`, …) so the repo keeps them.
- **Category**: *Personalization*. **Tags**: Wallpapers, Calendar, Productivity (pick the closest offered).
- **Contact email**: required and public. Use an address you are happy to publish.
- **Website**: https://nabin47.github.io/calwallpaper/

## Pre-launch checklist
✅ = in the repo and checked by CI or by reading the code. ☐ = needs the owner (phone or Play Console).

**Build and signing**
- ✅ AAB built and signed by `release.yml` from a `vX.Y.Z` tag; signature checked against the release keystore.
- ✅ `versionCode` derived from the tag and increasing (`v0.1.0` → 1000).
- ✅ Target SDK 37 (≥ 36 required), 64-bit ready (no native code), R8 and resource shrinking on.
- ✅ Baseline Profile generated in CI (`baseline-profile.yml`) and compiled into release builds.
- ☐ Release secrets present: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Keep an
  offline backup of the keystore: it is the **upload key**, and losing it means a key reset through Play support.
- ☐ Tag `v0.1.0` after this PR is merged and CI on `main` is green; the release then has the AAB.

**Manifest and behavior**
- ✅ `uses-feature android.software.live_wallpaper` (required), adaptive icon with a monochrome (themed) layer.
- ✅ Permissions as in the table above, nothing restricted; HTTPS only; no analytics, ads or tracking SDKs.
- ✅ Strings in `strings.xml`; no screen-orientation lock (Android 16 ignores it on large screens anyway).
- ✅ Accessibility: follows the system font size (non-linear on Android 14+), largest sizes tested for overlap,
  contrast warning in settings for every text color, labeled controls and headings for TalkBack, 48 dp targets.
- ☐ On the phone, with the release APK from the GitHub Release: onboarding, calendar permission, live
  wallpaper, lock-screen privacy, battery saver mode, widgets, taps, Todoist connect, Quick Settings tile.
- ☐ On the phone: Settings → Accessibility → font size at maximum. The settings screen and the wallpaper stay
  readable, nothing overlaps (screenshot please).
- ☐ On the phone: TalkBack on, swipe through the settings. Every control says what it is and its state.

**Listing and policy**
- ✅ Listing text, release notes (≤ 500 chars), icon and feature graphic in `fastlane/metadata/android/en-US/`.
- ✅ Privacy policy published by `pages.yml` and linked from the site.
- ☐ Privacy policy live: open https://nabin47.github.io/calwallpaper/privacy/ after the merge to `main`.
- ☐ At least 2 phone screenshots.
- ☐ All App content declarations answered as above.

**After the first upload**
- ☐ Read the **pre-launch report** (Play tests the app on real devices automatically). Expect it to stop at the
  calendar permission screen, which is fine. Fix crashes; review accessibility suggestions.
- ☐ Turn on automated uploads (below).

## Manual steps in the Play Console (in order)
1. **Developer account.** Create a personal account at https://play.google.com/console (one-time US$25 fee),
   verify your identity, phone number and email, and confirm access to a physical Android device through the
   Play Console app, as asked.
2. **Create the app.** *Create app* → name **CalWallpaper**, default language English (United States), *App*,
   *Free*, accept the declarations. The package name is fixed by the first upload.
3. **App content.** Fill in every declaration in "Other App content declarations" and "Data safety" above.
4. **Screenshots.** On the phone, take 2–8 screenshots: the home screen with the wallpaper, the lock screen
   (privacy on), the settings with the live preview, a widget. Upload them in *Main store listing*.
5. **Store listing.** Paste the texts, upload the icon and feature graphic, set category, tags and contact
   email.
6. **First AAB, by hand.** Tag `v0.1.0` (docs/release.md), download `CalWallpaper-v0.1.0.aab` from the GitHub
   Release, then *Testing → Closed testing → Create track/release → Upload*. On first upload Play asks about
   **Play App Signing**: keep the default (*Google generates and protects the app signing key*). Your keystore
   becomes the upload key. Paste the notes from `changelogs/1000.txt`.
   - Consequence: Play installs are signed by Google's key, GitHub Release APKs by your upload key. Android
     cannot update one with the other; switching between them needs an uninstall. That is expected.
7. **Review access for Todoist.** Make a free Todoist account just for review, copy its token (Todoist →
   Settings → Integrations → Developer) into *App access*, together with the instructions above.
8. **Closed test (new personal accounts).** Accounts created after 2023-11-13 must run a closed test with
   **at least 12 testers opted in for 14 days in a row** before they can apply for production. Add testers by
   email list or Google Group, share the opt-in link, and keep them opted in.
9. **Apply for production access** (Dashboard), answer the questions about the test, then roll out to
   Production when approved.
10. **Android developer verification.** In the Play Console, check that the account is verified and the app is
    registered. Before the global rollout planned for 2027, also register the upload key's certificate if
    asked, so the GitHub Release APKs keep installing on certified devices.

## Automated uploads (`play.yml`)
After step 6, uploads can run from GitHub.
1. Google Cloud console: create (or pick) a project, enable the **Google Play Android Developer API**, create a
   **service account**, and add a **JSON key** for it.
2. Play Console → *Users and permissions* → *Invite new users* → the service account's email. App
   permissions for CalWallpaper: *Release to testing tracks* (and *Release to production* if wanted), *View app
   information*.
3. GitHub → Settings → Environments → **google-play** (created on the first run, or create it now): add the
   secret `PLAY_SERVICE_ACCOUNT_JSON` with the whole JSON key, and optionally *Required reviewers* so every
   upload needs your approval. A repository secret of the same name also works.
4. Settings → Secrets and variables → Actions → *Variables*: `PLAY_UPLOAD_ENABLED` = `true`.
5. For each release: tag it (release.yml builds the AAB), then **Actions → Play upload → Run workflow** with
   the tag, the track (`internal`, `alpha` = closed testing, `beta`, `production`) and the status.

The workflow downloads the AAB from the GitHub Release (it never rebuilds), checks it against
`SHA256SUMS.txt`, takes the release notes from `changelogs/<versionCode>.txt` (or `default.txt`), adds the R8
mapping from the release run if it still exists, and uploads with `r0adkll/upload-google-play` (pinned by commit).
Store listing text is not uploaded automatically: paste changes into the Play Console.

For each new version, add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (≤ 500 chars).
