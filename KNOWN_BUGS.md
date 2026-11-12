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
