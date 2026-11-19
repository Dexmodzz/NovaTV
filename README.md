# NovaTV

A modern, dark interface for watching videos on Android TV, made for the remote control.

- Clean dark design (Dark or pure-black OLED), four accent colors, rounded cards with channel avatars
- Full-width search bar, icon side menu, centered player with round buttons
- Ad blocking and SponsorBlock built in
- Every setting in one place, including a dedicated **NovaTV** screen (theme, accent, cards per row,
  card preview, clock, animations, player title position)
- Available in 47 languages, following the device language
- Package `app.novatv` (beta `app.novatv.beta`, fdroid `app.novatv.fdroid`)

No YouTube/Google logo, name, icon or font is used: the font is Inter (SIL OFL, `assets/fonts/INTER_LICENSE.txt`).

---

## Contents

1. [Build and install](#1-build-and-install)
2. [What changed, phase by phase](#2-what-changed-phase-by-phase)
3. [File by file](#3-file-by-file)
4. [Translations](#4-translations)
5. [Pulling updates from the original project](#5-pulling-updates-from-the-original-project)
6. [Credits](#6-credits)

---

## 1. Build and install

**JDK:** OpenJDK **11–14** (AGP 7.4.2 needs at least 11; with newer JDKs the app may crash).

```bash
git clone --recursive https://github.com/Dexmodzz/NovaTV.git && cd NovaTV
# if it was cloned without --recursive:
git submodule update --init --recursive

JAVA_HOME=/path/to/jdk-11 ./gradlew assembleStstableDebug     # check build
JAVA_HOME=/path/to/jdk-11 ./gradlew assembleStstableRelease   # APK to install
```

APK: `smarttubetv/build/outputs/apk/ststable/release/NovaTV_stable_<version>_<abi>.apk`
(`arm64-v8a` for 64-bit Android TV devices, otherwise `armeabi-v7a` or `universal`).

```bash
adb connect <tv-ip>:5555
adb install -r smarttubetv/build/outputs/apk/ststable/release/NovaTV_stable_*_arm64-v8a.apk
```

> **Always install the release build.** The debug build turns on the network profiler and the full HTTP body
> logger in `SharedModules` (OkHttpCommons, `BuildConfig.DEBUG`): every chunk of video goes through the log,
> starting a video takes 6–11 s, memory fills up and a "source error" may appear. The release starts in about 0.6 s.
>
> **Signing:** if `keystore.properties` exists, the release uses that key; otherwise it is signed with the
> standard Android debug key. This way the release always builds and installs over a debug build without
> losing app data. To publish the app use your own key (`keystore.properties`); changing key later requires
> uninstalling the app.

---

## 2. What changed, phase by phase

| Phase | Content |
|---|---|
| **0 – Identity** | `app.novatv*` package, "NovaTV" name, `NovaTV_*` APK, new icon and banner, voice search authorities updated, in-app updater disabled (NovaTV never downloads the original project's APK), Firebase applied only when `google-services.json` contains the NovaTV package. |
| **1 – Design tokens** | Palette (background `#0F0F10`, surfaces `#1C1C1F`/`#26262A`, text `#F1F1F1`/`#AAAAAA`, orange/blue/violet/green accents), radii, spacing, type scale; `App.Theme.Nova.Dark/Oled` themes; Inter font on every screen; the 8 original color schemes keep name and position (the saved index stays valid) but map to Nova Dark or Nova OLED. |
| **2 – Header and side menu** | Header: logo, full-width search bar (with mic), clock + date, avatar. 112dp side menu with icon + label. Search screen with the same bar. The category *chips* were later removed on request: every section is back in the side menu. |
| **3 – Video cards and Home** | 14dp rounded cards on a dark surface, white ring + 1.05 zoom on focus, real channel avatar next to the title, "channel" and "views · date" on separate lines, accent-colored progress bar. Interruptible navigation (right then down no longer gets stuck while a row scrolls). Automatic restart after a scale change. |
| **4 – Player** | Round buttons (label only on focus), white Play/Pause with accent ring, thicker bar with times on both sides, centered layout, gradients; top header with title, avatar, channel · subscribers and clock; "SponsorBlock: N segments" chip; buttons stop at the row edges instead of jumping to the bar. |
| **5 – Settings** | New **NovaTV** tile in the settings grid: Dark/OLED theme, accent color, cards per row (3–6), card preview (off/muted/audio) and its delay, clock and 24h format, focus animations, player title position (top / above the video info). Settings tiles get a focus ring. |
| **After** | Readable logo (Inter SemiBold), launcher banner = header logo, full-width search bar on any screen size, search results right below the bar (which hides only when the results cover it), every NovaTV text translated. |

---

## 3. File by file

General rule: **new code lives in new files** (`novatv_*`, `Nova*`); upstream files only get small hooks,
always marked with a `NovaTV` comment. To find them all:

```bash
git grep -n "NovaTV" -- ':!README.md'
```

### 3.1 New files

**`common` module**

| File | Purpose |
|---|---|
| `common/.../prefs/NovaTVData.java` | Preferences added by NovaTV, in their own file (`novatv_prefs`): accent, preview delay, focus animations, player title position. Applies the accent color overlay. |
| `common/.../presenters/settings/NovaTVSettingsPresenter.java` | "NovaTV" settings screen. Where an original setting already exists (color scheme, grid scale, preview, clock, 12/24h) it writes that same setting. |
| `common/.../misc/NovaFonts.java` | Applies Inter to the `TextView`s inflated from layouts (`LayoutInflater` factory), on every Android version. |
| `common/src/main/res/values*/novatv_strings.xml` | Texts of the NovaTV screen, one file per language (generated, see [Translations](#4-translations)). |
| `common/src/main/res/drawable-nodpi/settings_novatv.png` | Icon (palette) of the NovaTV tile; generated by `tools/novatv/gen_settings_icon.py`. |

**`smarttubetv` module – Java**

| File | Purpose |
|---|---|
| `tv/ui/browse/NovaSections.java` | Split of sections between menu and chips. Chips are disabled (`CHIPS_ENABLED = false`): every section is in the side menu. |
| `tv/ui/playback/other/NovaPlayerHeader.java` | Player top header (title/channel and clock), with an interruptible fade together with the controls. |
| `tv/ui/playback/other/NovaPlayerTitle.java` | Fills title + avatar + "channel · subscribers" in the top header or in the block above the video info, depending on the setting. |
| `tv/util/NovaAvatars.java` | Real channel avatars on cards and in the player: cached per channel, at most 2 requests at a time, only for visible cards. |
| `tv/util/NovaInitialDrawable.java` | Colored circle with the channel initial, shown until the avatar is loaded. |

**`smarttubetv` module – resources**

| File | Purpose |
|---|---|
| `res/values/novatv_colors.xml` | Palette and overrides of the Leanback `lb_*` colors. |
| `res/values/novatv_dimens.xml` | Radii, spacing, text sizes, header/menu/player/search bar sizes; overrides of a few Leanback (`lb_*`) dimensions. |
| `res/values/novatv_themes.xml` | `App.Theme.Nova.*` themes, `novaAccent`/`novaChipBg`/`novaPlayBg` attributes, `NovaAccent.Orange/Blue/Violet/Green` overlays. |
| `res/values*/novatv_strings.xml` | UI texts (e.g. "SponsorBlock: %d segments"), one file per language (generated). |
| `res/drawable/novatv_*.xml` | Backgrounds: card, focus ring, pill, search bar, player buttons, chips, side menu, progress bars for each accent, launcher banner (`novatv_banner.xml`). |
| `res/color/novatv_*.xml` | Pill and chip text colors by state. |
| `res/layout/lb_control_button_secondary.xml` | Pill-shaped player button with a label (overrides the Leanback one). |
| `res/layout/novatv_player_title_block.xml` | Title/channel block above the video info line. |
| `res/mipmap-nodpi/novatv_logo.png` | Header logo, also used as banner and `android:logo`. |
| `assets/fonts/inter_*.ttf` + `INTER_LICENSE.txt` | Inter font (OFL). |

**Tools** (`tools/novatv/`, only needed to regenerate images and texts)

| File | Purpose |
|---|---|
| `gen_icons.py` | App icon (rounded square + play triangle). |
| `gen_logo.py` | Header logo with the wordmark in Inter SemiBold (needs `pycairo`). |
| `gen_settings_icon.py` | Icon of the NovaTV settings tile. |
| `gen_strings.py` | Translations of all the NovaTV texts; writes every `novatv_strings.xml`. |

### 3.2 Modified upstream files

**Build and identity**

| File | Change |
|---|---|
| `smarttubetv/build.gradle` | `applicationId` → `app.novatv*`, APK name `NovaTV`, Firebase only when configured for NovaTV, release signed with the debug key when `keystore.properties` is missing. |
| `smarttubetv/src/main/AndroidManifest.xml` | `android:banner` → `@drawable/novatv_banner`, `android:logo` → `@mipmap/novatv_logo`. |
| `smarttubetv/src/*/res/values/strings.xml` | App name. |
| `leanbackassistant/src/*/res/values/strings.xml`, `.../xml/searchable.xml` | Voice search authorities with the new packages. |
| `smarttubetv/src/*/res/mipmap-*/app_icon*.png`, `app_banner.png` | NovaTV icon and banner. |
| `common/.../dialogs/AppUpdatePresenter.java` | The in-app updater doesn't run for the NovaTV packages. |

**Themes and settings**

| File | Change |
|---|---|
| `common/.../prefs/MainUIData.java` | The 8 color schemes map to the Nova themes (names and order unchanged). |
| `common/.../misc/MotherActivity.java` | Applies the accent color overlay after the theme. |
| `common/.../misc/AppDataSourceManager.java` | Adds the NovaTV tile to the settings grid. |
| `common/.../settings/MainUISettingsPresenter.java`, `UIScaleSettingsPresenter.java` | Automatic restart after changing the UI or grid scale (without it the UI stayed cut above 1.3x). |
| `smarttubetv/.../ui/common/LeanbackActivity.java` | Focus zoom from `NovaTVData` (animations on/off). |
| `smarttubetv/.../util/ViewUtil.java` | `FOCUS_ZOOM_FACTOR` no longer `final`, so it can be configured. |
| `smarttubetv/.../presenter/SettingsCardPresenter.java` | Settings tiles with a focus ring. |
| `smarttubetv/src/main/res/values/colors.xml`, `dimens.xml` | A few values aligned with the palette and the narrow menu (`lb_browse_rows_margin_start`). |

**Header, menu, Home, search**

| File | Change |
|---|---|
| `res/layout/lb_title_view.xml` | Header rewritten: logo, full-width search bar with mic, clock/date, avatar. **All original ids are kept.** |
| `res/layout-ldrtl/lb_title_view.xml` | **Deleted**: the new layout mirrors itself for right-to-left languages. |
| `tv/ui/widgets/browse/NavigateTitleView.java` | Search bar, mic, remote control navigation in the header, "back to video" button that takes no space when not needed. |
| `tv/ui/browse/BrowseFragment.java` | Section selection, header hidden when the menu opens over scrolled rows. |
| `tv/presenter/IconHeaderItemPresenter.java`, `res/layout/icon_header_item.xml` | Side menu items (icon above, label below, rounded selection). |
| `tv/presenter/VideoCardPresenter.java`, `ShortsCardPresenter.java` | Rounded cards, channel avatar. |
| `tv/ui/widgets/complexcardview/ComplexImageCardView.java` | Avatar + channel row, two-line subtitle. |
| `tv/ui/widgets/complexcardview/ComplexImageView.java` | Preview delay from `NovaTVData`. |
| `res/layout/lb_search_bar.xml`, `res/drawable/rounded_search_bg.xml` | Search screen bar identical to the Home one (original ids kept). |
| `tv/ui/mod/leanback/search/SearchSupportFragment.java` | Results right below the bar; the bar hides only when the results cover it. |
| `tv/presenter/ChannelHeaderPresenter.java` | No app logo in the channel page search bar. |

**Player**

| File | Change |
|---|---|
| `res/layout/lb_playback_fragment.xml` | Top header (title, avatar, channel · subscribers, clock). |
| `res/layout/lb_playback_transport_controls_row.xml` | Centered layout, times on both sides of the bar, remaining time top right, SponsorBlock chip. |
| `res/drawable/player_background_controls.xml`, `player_background_suggestions.xml` | Gradients. |
| `tv/ui/playback/PlaybackFragment.java` | Shows/hides the top header together with the controls; shorter info line (channel and subscribers are in the title). |
| `tv/ui/mod/leanback/playerglue/tweaks/MaxControlsVideoPlayerGlue.java` | `NovaPlayerTitle` hook, centered texts. |
| `.../tweaks/PlaybackTransportRowPresenter.java` | SponsorBlock chip (counts the segments, chapters excluded). |
| `.../tweaks/ControlBar.java` | Minimum gap between buttons, left/right move one button at a time and stop at the edges, removed the `LayoutTransition` that made buttons lose focus. |
| `.../tooltips/ControlButtonPresenterSelector.java` | Button label visible only on focus, bigger white Play/Pause. |
| `tv/ui/mod/leanback/misc/SeekBar.java` | Bar colors (track and buffer). |

**Leanback library included in the repo**

| File | Change |
|---|---|
| `leanback-1.0.0/.../widget/GridLayoutManager.java` | One change (two spots marked `MOD: NovaTV`): in `onInterceptFocusSearch` the keys that change row are no longer dropped while the row is scrolling (interruptible navigation). |

**Untouched:** `MediaServiceCore/`, `SharedModules/` (submodules: APIs, ad blocking, SponsorBlock).

---

## 4. Translations

The app uses the device language, unless another one is chosen in Settings → Language/Country.
The base texts come from the original project; the NovaTV texts (the NovaTV settings screen, SponsorBlock chip,
voice search label) are translated into all the supported languages
(47 locales, plus the `iw`/`mo` aliases of Hebrew and Romanian). English is the fallback.

The `novatv_strings.xml` files are **generated**: edit the tables in `tools/novatv/gen_strings.py` and run

```bash
python3 tools/novatv/gen_strings.py
```

To add a NovaTV text: add the key to `COMMON_KEYS` (settings, `common` module) or `APP_KEYS` (UI, `smarttubetv`
module), add its translation to every language in the table, run the script. Number formats (e.g. "0.5 s" /
"0,5 s") come from the device locale.

---

## 5. Pulling updates from the original project

NovaTV is built on top of an open-source project (see [Credits](#6-credits)); its improvements and fixes can be merged in.
The model: a local `master` branch = exact copy of the original project (only on your computer, never pushed),
`novatv` = master + the NovaTV changes (the branch published here). Always update `master` first, then merge
`master` into `novatv`, then push `novatv`.

### 5.1 One time only

```bash
git clone --recursive https://github.com/Dexmodzz/NovaTV.git && cd NovaTV
# the original project, used only to pull updates
git remote add upstream https://github.com/yuliskov/SmartTube.git
git fetch upstream
git branch master upstream/master   # local copy of the original project

# Git remembers how you resolved a conflict and resolves it by itself next time
git config rerere.enabled true

# Don't touch line endings: many original Java files are CRLF
git config core.autocrlf false
```

### 5.2 Every update

```bash
git fetch upstream

# 1) master = upstream (fast-forward only: master holds none of our changes)
git checkout master
git merge --ff-only upstream/master

# 2) bring the news into novatv
git checkout novatv
git merge master

# 3) submodules at the version upstream points to (never modify them)
git submodule update --init --recursive

# 4) check
JAVA_HOME=/path/to/jdk-11 ./gradlew assembleStstableDebug assembleStstableRelease

# 5) publish
git push origin novatv
```

### 5.3 Why conflicts are rare and where they can happen

- Most of the work is in **new files** (`novatv_*`, `Nova*`): upstream doesn't know them, so they can't conflict.
- Modified upstream files almost always have **few lines**, marked `NovaTV`. A conflict there is solved by keeping upstream's new code and putting the `NovaTV` block back.
- Overridden Leanback resources (`lb_*.xml`) are app files with the same name as the library ones: a library update doesn't touch them.

Files where a conflict is possible, and how to solve it:

| File | What to do |
|---|---|
| `res/layout/lb_title_view.xml`, `lb_search_bar.xml`, `lb_playback_fragment.xml`, `lb_playback_transport_controls_row.xml` | They are rewritten. Keep the NovaTV version (`git checkout --ours <file>`) and add by hand any **new ids** introduced upstream (the Java code looks them up by id). |
| `res/layout-ldrtl/lb_title_view.xml` | If upstream changes it you get a "modified/deleted" conflict: keep it deleted (`git rm <file>`). |
| `NavigateTitleView.java`, `BrowseFragment.java`, `PlaybackFragment.java`, `ControlBar.java`, `SearchSupportFragment.java` | Keep upstream's changes and put the `NovaTV` blocks back. |
| `MainUIData.java` (color schemes) | If upstream adds a scheme, add it at the end pointing to `App.Theme.Nova.Dark` or `.Oled`, without changing the order of the others (the saved index must stay valid). |
| `build.gradle` | Keep upstream's news (version, dependencies) and the NovaTV changes: `app.novatv*` applicationId, `NovaTV` APK name, Firebase condition, fallback signing (`signingConfigs.debug`). |
| `README.md` | Keep the NovaTV version: `git checkout --ours README.md`. |
| `.png` icons/banners, `strings.xml` (`app_name`) | Keep the NovaTV version: `git checkout --ours <file>`. |
| `leanback-1.0.0/.../GridLayoutManager.java` | Look for `MOD: NovaTV` (two spots in `onInterceptFocusSearch`) and reapply the change. |
| New upstream locale (`values-xx`) | Add the language to `tools/novatv/gen_strings.py` and run it, otherwise NovaTV texts show in English there. |

During a merge:

```bash
git status                          # conflicting files
git checkout --ours  <file>         # keep NovaTV
git checkout --theirs <file>        # keep upstream
git add <file> && git commit        # finish the merge
git merge --abort                   # or cancel everything and go back to before the merge
```

> **CRLF files:** many Java files (e.g. `MotherActivity.java`, `AppDataSourceManager.java`, `ViewUtil.java`,
> `NavigateTitleView.java`, `MaxControlsVideoPlayerGlue.java`) use Windows line endings. Edit them with an
> editor that keeps line endings, otherwise the diff becomes the whole file and every later merge conflicts.
> To check: `file <path>` must say "with CRLF line terminators".

### 5.4 After the update

1. Build debug and release (see above).
2. Install the release on the TV and quickly check:
   - Home: header, side menu, cards, avatars;
   - search: bar and results;
   - player: top header, buttons, SponsorBlock;
   - Settings → NovaTV.
3. If upstream adds a new section to the menu, check that it has its icon in the side menu (`IconHeaderItemPresenter`).

---

## 6. Credits

NovaTV is based on **[SmartTube](https://github.com/yuliskov/SmartTube)** by **Yuriy Liskov (yuliskov)**,
released under the MIT License. All the core of the app (video APIs, playback, ad blocking, SponsorBlock,
settings) is his work; NovaTV adds the new interface on top of it. Thank you!

The original copyright notice is kept in [`LICENSE`](LICENSE), as the MIT License requires.

The Inter font is by Rasmus Andersson, released under the SIL Open Font License.
