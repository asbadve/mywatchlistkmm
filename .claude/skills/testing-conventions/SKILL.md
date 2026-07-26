---
name: testing-conventions
description: Test-coverage requirement and Compose UI testing patterns for this codebase - every new feature needs both a JUnit unit test and a Compose UI JUnit test. Apply when implementing any new feature or modifying ScreenModel/composable behavior.
---

# Testing conventions (user-mandated)

## Definition of done: every new feature needs both test types

From 2026-07-27 onward, any new feature implementation must add:
1. A **unit test** (`kotlin.test` / JUnit) for the business logic - ScreenModel state transitions,
   repository behavior, model/serialization logic. Pattern: `Fake<X>Repository` test double +
   `UnconfinedTestDispatcher()` via `Dispatchers.setMain()` in `@BeforeTest`/`resetMain()` in
   `@AfterTest`. See any existing `*ScreenModelTest.kt` for the shape.
2. A **Compose UI JUnit test** (`*UiTest.kt`, see below) for the composable itself - rendering,
   click-driven navigation callbacks, and any interactive state (filters, expand/collapse, retry).

This is the same tier as ktlint-clean and the exception/magic-string rules in
[[code-conventions]] - not optional, not something to ask about per-feature.

## Compose UI test setup

Use the **v2 API** — `androidx.compose.ui.test.v2.runComposeUiTest` (import exactly this, not the
deprecated `androidx.compose.ui.test.runComposeUiTest`). v2 is what JetBrains recommends going
forward (v1 is deprecated as of Compose Multiplatform 1.11.1) and is what every UI test in this
codebase uses as of 2026-07-27. `@OptIn(ExperimentalTestApi::class)` on the test class.

Tests live in `commonTest` (not a JVM-only source set) and run via
`./gradlew :composeApp:desktopTest` - `compose.uiTest` is already on `commonTest`'s dependencies
and `compose.desktop.currentOs` is explicit on `desktopTest`'s dependencies (see
`composeApp/build.gradle.kts`) specifically to keep the skiko native runtime resolvable
(this guards against JetBrains/compose-multiplatform#1352 - a 2021 bug where the test source
set's classpath didn't get the platform skiko binary at all). No Xvfb, no font packages, no
headless flags are needed - confirmed working on GitHub Actions' `ubuntu-latest` runner as-is;
`compose.desktop.currentOs` resolves per whatever machine is actually running Gradle.

### Wiring a real ScreenModel/ViewModel into a test

Composables that take a `viewModel: XScreenModel = viewModel(key = ...) { XScreenModel(id) }`
default parameter can bypass the `viewModel()` factory (and the `LocalViewModelStoreOwner`/
`LocalLifecycleOwner` ceremony that needs) entirely - just construct the real ScreenModel
directly with a `Fake<X>Repository` and pass it as the explicit `viewModel =` argument:

```kotlin
val fakeRepository = FakeMovieRepository().apply { getMovieDetailsResult = Result.success(...) }
val viewModel = MovieDetailScreenModel(movieId = 1, movieRepository = fakeRepository)
setContent { MovieDetailScreen(movieId = 1, windowSize = WindowSize.COMPACT, viewModel = viewModel, ...) }
```

Set `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@BeforeTest` the same as ScreenModel
unit tests - this makes the ViewModel's `init { load...() }` (and any nested
`coroutineScope { async {}.awaitAll() }` fan-out, e.g. TvDetailScreenModel's per-season fetch)
resolve synchronously before `setContent` even runs, no `runTest`/`advanceUntilIdle()` needed.

### Known pitfalls (all found the hard way this session)

- **Off-screen `LazyColumn`/`LazyRow` items are composed but not click-reachable.** Compose
  pre-composes items near the viewport edge for prefetch, but a node whose bounds fall outside
  the visible clipped area will report `Actions = [OnClick, ...]` and accept `performClick()`
  without throwing - the click just silently no-ops (coordinates land outside the rendered
  surface). Symptom: your callback var stays `null`/`false` with no exception. Fix: scroll the
  list into view first:
  ```kotlin
  onNode(hasScrollToIndexAction()).performScrollToIndex(itemIndex)
  ```
  If that throws "expected exactly 1 but found 2" (a nested `LazyRow`, e.g. a cast rail, also
  matches `hasScrollToIndexAction()`), disambiguate by picking the first match - the outer list
  is always listed first in traversal order:
  ```kotlin
  onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(itemIndex)
  ```

- **Duplicate text throws, not silent failure.** `onNodeWithText`/`assertExists` require *exactly
  one* match. A screen's title showing in both the `TopAppBar` and the body, or the same credit
  appearing in two sections (e.g. `PersonDetailScreen`'s "known for" pool overlapping its
  filmography list), throws `Expected exactly '1' node but found '2'`. Use
  `onAllNodesWithText(text)[0]` instead of asserting/clicking a plain `onNodeWithText(text)` any
  time the same string could legitimately render twice.

- **A Material3 `TextButton`'s click can silently not fire in some embedded/animated contexts.**
  Hit once with `TvDetailScreen`'s "View All" button (nested inside a scrolled `LazyColumn` with
  an `animateColorAsState`-driven header) - the node existed, `performClick()`/
  `performTouchInput { click() }` didn't throw, but the callback never ran. A plain `.clickable()`
  row triggering the *same* callback (the season row itself) worked immediately. If a button click
  silently no-ops after ruling out the off-screen and duplicate-text causes above, look for an
  alternate clickable target wired to the same callback before assuming the app has a real bug.

- **A ViewModel-rendered field with no corresponding UI is a real gap, not a test bug.**
  `TvDetail.tagline` exists on the model but `TvMetaSection` never renders it (unlike
  `MovieMetaSection`, which does) - don't invent an assertion for data the screen doesn't
  actually display; that's a product feature-parity gap worth flagging, not something to fake
  around in the test.
