# Known bugs

Reproducible defects with a known symptom, tracked here so they survive across sessions.
Engineering/ops work-in-progress lives in the (untracked) `TASKS.md`; product ideas live in
`future_features_checklist.md`, and features that already shipped are archived in
`shipped_features.md`.

Defects to fix first, before any other work, live in `IMMEDIATE_DEFECTS.md`.

Each entry: what you see, where it is, what has been ruled out, and the next concrete step.

---

## Android — Discover filter dropdowns open at the far left of the screen

**Symptom.** In Movies/TV → Discover → Filters, tapping the Year or Sort by field opens its menu
at the left edge of the screen, narrower than the field and outside the dialog. Seen 2026-10-17 on
the Pixel 10 Pro Fold emulator during item 20's runtime checks; iOS and desktop place it correctly.
Selecting still works.

**Where.** `features/discover/screen/DiscoverFilterDialog.kt` puts a generic `DropdownMenu` inside
`ExposedDropdownMenuBox`. Material's `ExposedDropdownMenu` (from the box's scope) is the component
that anchors to the field and matches its width.

**Next step.** Swap both `DropdownMenu`s for `ExposedDropdownMenu`, then re-check on Android in both
orientations and both themes.

## Offline favorite/watchlist changes are never sent to TMDB

**Symptom.** Favorite, unfavorite, watchlist or un-watchlist a title while offline: the icon keeps
the new state and the local lists follow it, but TMDB never hears about it. Back online, the next
complete refresh puts the lists back to what TMDB has - an offline removal comes back, an offline
addition disappears. Found 2026-10-17 while fixing the Upcoming-timeline defect; not reported by a
user yet.

**Where.** `MediaActionsState` treats a network failure as `ToggleOutcome.OFFLINE` and leaves the
change "queued": a removal is marked `isDeleted = 1, pendingSync = 1` in `trackedMedia`, an
addition is written locally by `TrackedMediaRepository.trackLocally`. Its comments say a sync
flushes the queue, but nothing does - no code reads `pendingSync` back, retries `setFavorite`/
`setWatchlist`, or keeps queued additions at all. `TrackedMediaRepository.refreshAll` only skips
`pendingSync = 1` rows when pruning, so queued removals stay hidden locally forever while TMDB
still has the title.

**Next step.** Add a real outbox: record each offline toggle (media, category, desired on/off),
flush it before `refreshAll` prunes (and from the background job), and only then reconcile with
TMDB. Don't overload `pendingSync` for additions - today it means "removal pending", so a flush
written against it would send an offline addition as a delete.

## Android — open screens are lost when the Activity is recreated (e.g. theme change)

**Symptom.** With the Account dialog open, switching the device between light and dark theme
closes it: the app comes back on Trending. Found 2026-10-17 by the Android CLI journey run on the
Pixel 10 Pro Fold emulator (`adb shell cmd uimode night yes|no`); still closed 10 s later, so it's
not a redraw in progress. Any open detail screen is very likely lost the same way.

**Where.** `App.kt` builds the navigation state with a plain
`remember { TopLevelBackStack(TrendingKey) }` (`App.kt:177`). `remember` doesn't survive Activity
recreation, and `TopLevelBackStack` (`navigation/NavigationRoutes.kt`) holds its stacks in an
`ArrayDeque`/`mutableMapOf` that nothing saves, so a recreated Activity starts again from
`TrendingKey`. Scroll state inside the destination *is* restored (`rememberScrollState` is
saveable), which is why the reopened dialog came back already scrolled.

**Ruled out / scope.** Rotation and fold/unfold are probably fine: `AndroidManifest.xml` declares
`configChanges="orientation|screenSize|screenLayout|keyboardHidden"`, so those don't recreate the
Activity. What does: `uiMode` (theme), locale, font scale/density, and process death after the app
was in the background.

**Next step.** Make the back stack saveable - Navigation 3's `rememberNavBackStack` per top-level
tab, or a `Saver` for `TopLevelBackStack` used with `rememberSaveable`. Either way the keys need
`@Serializable` first: `AppKey` and its subtypes are `NavKey`s but aren't annotated today. Load the
`navigation-3` skill first. Verify with the same theme toggle with the Account dialog and a detail
screen open, plus "Don't keep activities" for process death.

## Lists ask TMDB for one page past the last (found 2026-10-25)
**What you see:** nothing visible - one extra, empty request each time a list reaches its end.
**Where:** `MovieListScreenModel`, `DiscoverMovieScreenModel`, `TvListScreenModel`,
`DiscoverTvScreenModel`, `PersonListScreenModel` set `canPaginate = response.page <= response.totalPages`.
On the last page `page == totalPages`, so it stays `true` and scrolling to the end requests
`totalPages + 1`. `SearchScreenModel` already uses `<`.
**Ruled out:** not the double page-1 fetch - that was a separate guard bug, fixed the same day
(`isLoadInFlightOrExhausted()`). The UI tests work around this with `totalPages = 0` in their fakes.
**Next step:** change `<=` to `<` in the five models, drop the `totalPages = 0` workaround from
the tests, add a unit test that the last page doesn't paginate.

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
