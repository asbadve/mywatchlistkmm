# Known bugs

Reproducible defects with a known symptom, tracked here so they survive across sessions.
Engineering/ops work-in-progress lives in the (untracked) `TASKS.md`; product ideas live in
`future_features_checklist.md`, and features that already shipped are archived in
`shipped_features.md`.

Each entry: what you see, where it is, what has been ruled out, and the next concrete step.

---

## IMMEDIATE — a movie favorited from Trending doesn't show up in the Upcoming timeline

**Priority.** Fix next, before other work. Reported 2026-10-10.

**Symptom.** Open an unreleased movie from Trending, tap the favorite (heart) icon, then go to
My Favorites → Upcoming. The movie isn't on the timeline. It only shows up later, after some full
sync happens to pick it up.

**Where.** The Upcoming timeline reads only the local `trackedMedia` table
(`UpcomingReleasesScreenModel.upcomingItems` → `TrackedMediaRepository.observeUpcoming` →
`selectUpcomingTrackedMedia` in `MyDatabase.sq`). Favoriting from a detail screen goes through
`core/ui/hero/MediaActionsState.toggleFavorite`. When it turns a favorite **on**, a successful
call only runs `trackedMediaRepository.clearPendingDelete(...)`. That clears flags on a row that
already exists, but it never *inserts* one. So a title favorited for the first time writes nothing
local, and the timeline has nothing to show. `toggleWatchlist` has the same path, so a watchlisted
movie is very likely affected too. The bug isn't specific to Trending: Trending is just the usual
way to reach a movie you haven't favorited yet.

**Why the existing safety net misses it.** `MyFavTabs` runs `upcomingViewModel.refresh(...)`
(→ `TrackedMediaRepository.refreshAll`) every time the Upcoming tab is shown. That re-pulls the
favorite/watchlist lists from TMDB. It still misses the new favorite when (a) the effect doesn't
re-run, because the tab's `LaunchedEffect(upcomingViewModel)` is keyed on a ViewModel that outlives
tab switches, or (b) TMDB's `/account/{id}/favorite/movies` still returns the list from before the
favorite, right after the write. Neither has been confirmed yet. Either way, the real fix is the
missing local write: the refresh is a backstop, not the mechanism.

**Fix direction.**
1. On favorite/watchlist **on** with `ToggleOutcome.SUCCESS` (and on `OFFLINE`, with `pendingSync`
   set, so it still shows up when offline), upsert the row with the existing `upsertTrackedMedia`
   query. `MediaActionsState` only knows `mediaType`/`mediaId` today, so it also needs the title,
   poster path, release date and vote average. The owning `MovieDetailScreenModel` /
   `TvDetailScreenModel` already holds the detail and can pass them in.
2. Add a `TrackedMediaRepository` method for that single-row upsert, so `MediaActionsState` doesn't
   reach into the queries directly.
3. Tests (per testing-conventions): a unit test that favoriting a new unreleased movie makes it
   appear in `observeUpcoming()` with no `refreshAll`; plus a UI test that tapping the heart on the
   movie detail hero, then opening Upcoming, shows the title.
4. Once that's done, check whether (a) above also needs fixing, e.g. key the refresh effect on tab
   visibility instead of the ViewModel.

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
