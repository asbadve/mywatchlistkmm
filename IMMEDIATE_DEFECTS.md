# Immediate defects

Defects to resolve **first** at the start of the next dev session, before any other work.
When one is fixed, delete its entry here and note the fix in the commit message. Lower-priority
bugs live in `KNOWN_BUGS.md`.

---

## A movie favorited from Trending doesn't show up in the Upcoming timeline

Reported 2026-10-10.

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
