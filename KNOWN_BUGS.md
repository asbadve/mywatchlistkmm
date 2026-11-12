# Known bugs

Reproducible defects with a known symptom, tracked here so they survive across sessions.
Engineering/ops work-in-progress lives in the (untracked) `TASKS.md`; product ideas live in
`future_features_checklist.md`, and features that already shipped are archived in
`shipped_features.md`.

Defects to fix first, before any other work, live in `IMMEDIATE_DEFECTS.md`.

Each entry: what you see, where it is, what has been ruled out, and the next concrete step.

---

## Favorites grid blank for ~10s after returning from an offline toggle
**What you see:** offline, favorite a title on its detail page, then go back to My Fav >
Favorites: the grid is blank with a spinner for about 10 seconds before the saved titles reappear
(with the new "not synced" marker). Seen 2026-11-12 on the Android emulator (Pixel 10 Pro Fold).
**Where:** `AccountFavoritesWatchlistTab` / `TrackedMediaRepository.pagedFlow`. It follows the local
write (`trackLocally`) that invalidates the grid's `QueryPagingSource`.
**Ruled out:** not the grid's own network refresh - `TrackedMediaRemoteMediator.initialize()` now
skips it when rows are saved, and a plain tab switch offline (no new write) shows the grid at once.
**Next step:** log `loadState` (source and mediator, refresh/append) while reproducing, to see whether
the reload after invalidation is waiting on something - for example the detail screen's still-running
`account_states` call or detail-cache refresh holding the database.

## Movie/TV detail pages take ~1 minute to show their cached copy offline
**What you see:** offline, opening a movie whose detail page was cached shows a spinner for about a
minute, then the cached page. A page that was never cached never loads. Seen 2026-11-12 on the
Android emulator; present before the offline-first work too.
**Where:** the detail screens' `NetworkBoundResource`-style detail cache. The likely cause is the same
as the Favorites grid's (fixed 2026-11-12): the cached copy is only shown after the network fetch fails,
and offline that takes as long as `TmdbClient`'s `HttpRequestRetry` (3 retries, growing delays).
**Next step:** emit the cached copy first and refresh in the background (offline-first reads, per
developer.android.com/topic/architecture/data-layer/offline-first), or skip the retries for an
`UnknownHostException`, which means no network rather than a flaky one.

## Favorite/watchlist hearts show empty offline for titles that are favorited
**What you see:** offline, a detail page for a title in your favorites or watchlist shows an empty
heart/bookmark. Tapping it queues an "add" (harmless on TMDB, but wrong in the UI). Seen 2026-11-12.
**Where:** `MediaActionsState.load()` gets the icon state only from TMDB's `account_states`; offline
that call fails and the icons stay at their default (off).
**Next step:** when `account_states` fails, fall back to the local `trackedMedia` rows (not
`isDeleted`) plus any queued toggle for the title, then replace them with TMDB's answer once online.

## Avengers: Doomsday shows an empty heart online although it is a favorite
**What you see:** online, its detail page shows an empty heart even after fully loading, while
TMDB's favorites list (and the local grid) include it. Free Guy and Ramayana showed the right state the
same session. Seen 2026-11-12 on the Android emulator; not investigated.
**Next step:** check the `account_states` response for movie 1003596 (an unreleased title) and how
`MediaActionsState.load()` maps it.

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
