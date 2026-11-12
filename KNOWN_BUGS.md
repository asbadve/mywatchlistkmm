# Known bugs

Reproducible defects with a known symptom, tracked here so they survive across sessions.
Engineering/ops work-in-progress lives in the (untracked) `TASKS.md`; product ideas live in
`future_features_checklist.md`, and features that already shipped are archived in
`shipped_features.md`.

Defects to fix first, before any other work, live in `IMMEDIATE_DEFECTS.md`.

Each entry: what you see, where it is, what has been ruled out, and the next concrete step.

---

## Favorites grid blank for ~10s after returning from an offline toggle (emulator only so far)
**What you see:** offline, favorite a title on its detail page, then go back to My Fav >
Favorites: the grid was blank with a spinner for about 10 seconds before the saved titles came back.
Seen once, 2026-11-12, on the Android emulator (Pixel 10 Pro Fold AVD).
**Not reproduced on hardware:** on a physical Android device, the same steps - a favorite and a
watchlist toggle, each followed by going back - brought the grid back in ~30ms both times (temporary
`loadState` logging: source Loading -> NotLoading with 31 items). The emulator was badly loaded at the
time (17s on the splash screen right after), so this is most likely emulator slowness.
**Fixed alongside:** the logging showed the combined `refresh` reads NotLoading for a frame while the
local read is still running, which could flash "No favorites yet"; `accountMediaGridContent` now
waits for both the source and mediator refresh.
**Next step:** close this if it doesn't reappear on a real device; if it does, log
`loadState.source`/`loadState.mediator` again around the return to the grid.

## Detail screen keeps a stale favorite/watchlist state when reopened
**What you see:** open a movie offline, go back, go online and open the same movie again: the
favorite/watchlist icons keep the first visit's state instead of asking TMDB. Seen 2026-11-12 on the
emulator as an empty heart on Avengers: Doomsday (a favorite) while online. Not a TMDB problem: on a
physical Android device, `account_states` for movie 1003596 returned `"favorite":true,"watchlist":true` and the
icons showed it.
**Where:** `MovieDetailScreen`/`TvDetailScreen` get their ScreenModel with
`viewModel(key = "MovieDetailScreenModel:$movieId")`, and `NavDisplay` (App.kt) has no ViewModel
entry decorator, so every detail ScreenModel is owned by the Activity: reopening a title reuses it,
and `MediaActionsState.load()` only runs once, at creation. It also keeps every detail ScreenModel
opened in a session in memory.
**Mitigated 2026-11-12:** the icons now start from the local favorites/watchlist, so the offline
first visit shows the right state; a state changed elsewhere (e.g. on the TMDB website) is still
not picked up on reopen.
**Next step:** add `rememberViewModelStoreNavEntryDecorator()` to `NavDisplay`'s `entryDecorators`
(the `lifecycle-viewmodel-navigation3` dependency is already declared), following the navigation-3
skill. Check first that switching top-level tabs doesn't drop the other tabs' entries - if it does,
their ScreenModels would be cleared and reloaded on every tab switch.

## Android tests — 4 region-picker UI tests are skipped (Compose test-framework race)
**What you see:** on an Android device/emulator, 4 of the 5 `RegionPickerDialogUiTest` tests report
"skipped": the three that type into the dialog, plus the tap-a-region test, which hit the same race
on the slower CI emulator (2026-10-25, PR #23). They still run on desktop and iOS.
**Where:** interacting with a dialog intermittently crashes the test process with
`IllegalArgumentException: performMeasureAndLayout called during measure layout` - the test
thread measures while the main thread lays out the dialog window. Not app code:
https://issuetracker.google.com/issues/325299275. Skipped with `skipOnAndroid(...)`.
**Next step:** when androidx Compose UI ships a fix (or this project's version moves past it),
remove the `skipOnAndroid` calls and rerun `connectedDebugAndroidTest` a few times.

## Web (JS) — navigation icon not visible

**Symptom.** The navigation (back) icon does not render on the JS/browser target. Reported
2026-08-06. Android, desktop and iOS all show it correctly, so this is target-specific rather than a
regression in the shared composable.

**Where.** Every detail screen now draws its back affordance through one component,
`core/ui/DetailTopBar.kt`, using `Icons.AutoMirrored.Filled.ArrowBack`. The app-level search bar and
`SearchScreen` have their own icons, so a first useful data point is whether *those* icons render on
web too - if they also fail, this is about icon loading in general, not `DetailTopBar`.

**Ruled out.** Not a missing dependency: `libs.material.icons.core` is declared in `commonMain`
(`composeApp/build.gradle.kts:63`), so the JS target inherits it.

**Worth checking, in order.**
1. Whether *any* `androidx.compose.material.icons` vector renders on web, or only this one - 21
   files in `commonMain` import from that package, so a broad failure would be obvious.
2. `Icons.AutoMirrored.*` specifically. The auto-mirrored variants resolve through a different path
   than plain `Icons.Filled.*`; swapping one call site to `Icons.Filled.ArrowBack` is a two-minute
   test of that theory.
3. Tint. `DetailTopBar` tints the icon `onSurface` when solid and white over a hero image. An icon
   drawn in a colour matching its background is invisible rather than absent - check the DOM/canvas
   for a node of the right size before assuming it never drew.

**Blocked on.** Verifying any of this needs the browser target to render at all - see the blank-page
bug below, which has to be fixed first.

## Web (JS) — blank page

**Symptom.** `./gradlew :composeApp:jsBrowserDevelopmentRun` serves on `:8080` but renders nothing.

**Partial diagnosis.** `index.html` carries a stale `<script src="skiko.js">` tag; skiko 0.144.6
ships `skiko.mjs`, meant to be bundled by webpack. That alone should not blank the page, so a second
unidentified 404 is still outstanding.

**Next step.** Log 400+ responses via `page.on('response')` in puppeteer/Chrome DevTools against
`localhost:8080` to find the real missing asset, then fix the stale tag and whatever else 404s.
