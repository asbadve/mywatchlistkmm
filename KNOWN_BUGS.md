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
