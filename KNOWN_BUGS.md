# Known bugs

Reproducible defects with a known symptom, tracked here so they survive across sessions.
Engineering/ops work-in-progress lives in the (untracked) `TASKS.md`; product ideas live in
`future_features_checklist.md`, and features that already shipped are archived in
`shipped_features.md`.

Defects to fix first, before any other work, live in `IMMEDIATE_DEFECTS.md`.

Each entry: what you see, where it is, what has been ruled out, and the next concrete step.

---

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
