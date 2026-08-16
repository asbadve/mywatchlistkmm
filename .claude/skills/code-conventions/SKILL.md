---
name: code-conventions
description: Coding conventions for this codebase - specific exception types only (never catch/throw bare Exception) and no magic strings (constants for internal strings, compose string resources for user-facing text). Apply to ALL new or modified Kotlin code.
---

# Code conventions (user-mandated)

Apply these to every piece of new or modified Kotlin code. The codebase-wide cleanup was done
on 2026-07-21; keep it that way.

## 1. No generic exceptions

Never catch or throw bare `Exception`/`Throwable`. Catch the specific failures a call can
actually produce. In this codebase the usual suspects are:

- `HttpExceptions` (project type, `network/exception/HttpExceptions.kt`) - non-2xx API responses
- `io.ktor.utils.io.errors.IOException` - connectivity failures (also covers ktor's timeout types)
- `io.ktor.serialization.ContentConvertException` - ktor-side body conversion failures (this is
  what a malformed JSON payload actually throws through `body()`; catch it alongside...)
- `kotlinx.serialization.SerializationException` - direct kotlinx parsing failures
- `IllegalArgumentException` / `IllegalStateException` - bad input parsing, platform API misuse

If a truly-unknown failure path must be tolerated (e.g. best-effort parallel fetches), still
enumerate the known types; do not add a `catch (e: Exception)` fallback. The existing
`@Suppress("detekt:TooGenericExceptionCaught")` blocks are legacy - do not copy that pattern
into new code.

## 2. No magic strings

- **Not user-facing** (log tags, API paths/params, TMDB job/department names, map keys):
  declare a `private const val` in the class/file (or `private companion object` for classes).
  Example: `private const val TAG = "CollectionDetailScreenModel"`.
- **User-facing** (anything rendered in the UI - titles, labels, buttons, error messages):
  goes in `composeApp/src/commonMain/composeResources/values/strings.xml` and is read with
  `stringResource(Res.string.<key>)` from `org.jetbrains.compose.resources.stringResource`
  (generated accessors live in `mywatchlist.composeapp.generated.resources`).
  Keys are snake_case (`featured_cast`). Rebuild regenerates `Res.string` accessors
  (`./gradlew :composeApp:generateComposeResClass` or any compile).

Strings built from data (e.g. `"Directed by $names"`) should keep the template in the
resource file where practical (`%1$s` placeholders via `stringResource(res, arg)`).

## 2b. No magic numbers either - and where each constant belongs

Same rule as strings, applied to numbers that carry meaning. **Scope (agreed 2026-08-06):
*semantic* values only** - aspect ratios, image target widths, scrim alphas, limits, separators,
API values, thresholds. One-off layout literals (`padding(16.dp)`, `fontSize = 13.sp`) stay
inline; extracting every dp puts padding a screen away from the composable using it and makes the
code harder to read, not easier.

**Constants always live inside a class or object - never loose at file top level.** A bare
top-level `private const val` has no owner and reads as a stray value; grouping them names the
thing they configure and keeps them together as that thing grows.

Where a constant lives follows from how many places use it:

1. **One class** - `private companion object` inside that class.
2. **One file of top-level functions** (composables, extensions - no class to hang them on) -
   a `private object <Area>Constant` at the top of that file. Not loose `const val`s.
3. **Two or more files** - a shared `object <Area>Constant`, internal or public as needed. Never
   redeclare the same meaning-and-value in a second file; that is the duplication this rule
   exists to stop.

```kotlin
// WRONG - loose at file level
private const val AVATAR_SIZE_DP = 96
private const val ROLE_SEPARATOR = " · "

// RIGHT - owned by an object that names what they configure
private object PersonHeroConstant {
    const val AVATAR_SIZE_DP = 96
    const val ROLE_SEPARATOR = " · "
}
```

Shared constants objects follow the existing `object <Area>Constant` pattern
(`NetworkConstant`, `MoviesConstant`, `TvShowsConstant`, `NavigationConstants`,
`ConfigurationConstants`, `HeroConstant`). Put them in the narrowest package that covers every
caller - `core/constant/` when callers span features, the feature package when they do not.

## 2c. Don't duplicate logic - extract it

If the same logic appears in two places, it moves out into one named thing rather than being
copy-adjusted. For behaviour (not layout), prefer a **use case**: a small single-purpose class
with an `operator fun invoke`, named for what it does - `FindYoutubeTrailerUseCase`,
`ResolveWatchOptionUseCase`. Shared composables go in `core/ui/`, shared formatting in
`core/format/`.

The trigger is duplicated *logic*, not duplicated shape. Two composables that happen to both draw
a Row are fine; two copies of "pick the best streaming provider for this region" are not.

## 2d. Test files follow the same constant rules (agreed 2026-08-13)

Tests are not exempt. The same values keep reappearing across a feature's tests - a provider name,
a region code, the text of a string resource being asserted on - and a typo'd copy in one file
produces a test that passes while asserting the wrong thing.

Placement in a test mirrors the production rule, keyed on the *feature package*:

1. **Used in one test class only** - `private companion object` inside that class. Not loose
   top-level `private const val`s, which is what test files drift into. Note this pulls the file's
   fixture builders into the class too, since a `private companion object` is not visible to
   top-level functions in the same file.
2. **Used by two or more test files in the same feature package** - a shared
   `object <Area>TestConstant` in that package's test source, following the production
   `object <Area>Constant` naming. Put it in the narrowest package covering every caller: hero
   fixtures shared by the movie and TV hero tests live in `core/ui/hero/`, not in either feature.

Where production already declares the value, **import it rather than restating it** - a test
asserting on region "US" uses `RegionConstant.US`, not a private copy. The exception is a value
that *is* the contract under test: `FindYoutubeTrailerUseCaseTest` declares its own YouTube URL
prefix on purpose, because a test that imports the constant it is verifying asserts nothing.

**What stays inline:** one-off test data with no meaning beyond the case that uses it - a runtime
of 148, a title of "Inception", a vote count of 30_000. Naming every literal in a fixture makes
the test unreadable and hides what is actually being asserted. The trigger for extraction is
repetition or contract, exactly as in production code.

## 2e. Check the platform before writing a custom solution (agreed 2026-08-15)

Before building any mechanism, find out whether Compose, Material3, Kotlin or the platform already
provides it - and if a custom one already exists here, check whether the platform has since grown
the same capability. Read the API's parameters, not just its name: the thing you need is often an
argument on a function already being called.

Custom code is not banned. It is a decision that has to be *earned*, and the KDoc has to say what
the platform equivalent was and why it did not fit - `CollapsibleBarState` does this, naming
`enterAlwaysScrollBehavior` and explaining it cannot drive a bar living outside `Scaffold(topBar =)`.
Without that note, the next person cannot tell a deliberate choice from an unresearched one.

### Check the current docs, not just what you remember

Recalled API knowledge goes stale, and an assistant's training cutoff is usually older than the
versions here. **Look the API up online against the version this project actually uses** before
concluding the platform cannot do something:

- Read the version first - `gradle/libs.versions.toml` (as of 2026-08-15: Kotlin 2.3.21,
  Compose Multiplatform 1.11.1, plus the `material3Adaptive*` entries, which move independently).
- Fetch the current reference for the exact symbol, and check the **release notes** between the
  version you remember and the one declared here. A capability that did not exist when you last
  looked is the common case, not the rare one - this is how `canScroll` was missed.
- Prefer the API reference and release notes over blog posts and Stack Overflow, which are usually
  pinned to an older version and will happily confirm a limitation that has since been lifted.
- The dependency sources are also on disk and are the final authority when docs are ambiguous; the
  ProGuard investigation in `TASKS.md` was settled by reading a plugin's own source.

A "the framework can't do this" conclusion reached from memory alone is not a finding, and should
not be written into a KDoc as the justification for custom code.

Two failures from one bug on 2026-08-15, both worth recognising:

1. **A parameter that was already there.** Every `TopAppBarDefaults.enterAlwaysScrollBehavior()`
   call site left `canScroll` at its default `{ true }`, so top bars collapsed on screens with
   nothing to scroll. Material3 had shipped the fix for that exact problem; nobody had read the
   signature.
2. **Reimplementing a contract subtly wrong.** `CollapsibleBarState` is a legitimate custom
   mechanism, but it observed `available` in `onPreScroll` - what the gesture *offered* - where the
   platform's own bars observe what the list *consumed*. Rewriting a framework behaviour means
   inheriting its edge cases, and those are exactly what gets missed.

The tell for both: a bug that reproduces on our custom mechanism *and* on the framework's is
usually one misunderstanding, not two.

## 3. No wildcard imports

Never use `import foo.*` - always import each symbol explicitly (matches the style of the rest
of the codebase and keeps symbol origins greppable). The legacy Compose wildcards
(`androidx.compose.foundation.layout.*`, `material3.*`, `runtime.*`) were expanded on 2026-07-22.

## 4. ktlint must be clean before finalizing

Run `./gradlew :composeApp:ktlintCheck` before declaring any change done - zero violations is
the bar (the whole codebase was brought to zero on 2026-07-22). `./gradlew :composeApp:ktlintFormat`
auto-fixes most findings. Config lives in `.editorconfig` (Composable naming exemption, generated
sources under `build/generated` disabled); the plugin is wired in `composeApp/build.gradle.kts`.
Notable non-auto-fixable rules: PascalCase file names, no SCREAMING_CASE for mutable properties,
no empty files.

## 5. Error text from screen models: UiText

Screen models are not composable, so they never hardcode user-facing error strings. State
carries `com.ajinkyabadve.kmmmywatchlist.core.UiText` instead:
- `UiText.Resource(Res.string.error_network)` for localized messages (accessing `Res.string.*`
  outside a composable is fine - only `stringResource()` is composable),
- `UiText.Plain(httpExceptions.message)` for dynamic server-provided text.
Screens render it with `state.message.asString()`. Tests assert `UiText` values, not raw strings.

## 6. No API calls from a composable, and the composable never decides *when* to load

A `@Composable` function never calls a repository directly - not from its body, not from a
`rememberCoroutineScope().launch { }` inside a click handler, and **not indirectly either**, by
calling a state holder's load method from a `LaunchedEffect` keyed on "this just appeared." Both
mix network/IO triggering into the render function: the call re-fires on every recomposition key
change the composable happens to pick, survives no configuration change, and cannot be unit tested
without standing up the whole Compose test harness.

The ViewModel decides when to fetch, not the composable's lifecycle. Concretely: a ViewModel
subscribes to whatever *causes* a load (auth session appearing, a navigation arg changing) in its
own `init { }` via `viewModelScope.launch { repository.someFlow.collect { ... } }`, and the
composable only ever renders `uiState` and dispatches click events - see
`MovieDetailScreenModel`/`TvDetailScreenModel`'s `init` blocks, which subscribe to
`AuthRepository.sessionState` and call `mediaActionsState.load(session.sessionId)` themselves the
moment a session exists, rather than a `LaunchedEffect(session) { ... }` in `MediaActionButtons`
doing it.

A `ViewModel` (this codebase's convention names it `<Feature>ScreenModel`, see `AuthScreenModel`,
`AccountMediaListScreenModel`, `MovieDetailScreenModel`) owns every repository call, exposes a
`StateFlow<UiState>`, and does its own `viewModelScope.launch { }` (this codebase declares that
scope explicitly as `CoroutineScope(Dispatchers.Main)` rather than using androidx's built-in
property - match the existing files, don't mix the two styles). The composable:
1. Obtains the ScreenModel via `viewModel(key = "...") { FooScreenModel(...) }` (or takes one as an
   injectable default parameter for tests - see `testing-conventions`).
2. Reads `val uiState by screenModel.uiState.collectAsState()`.
3. Dispatches user events as plain method calls (`onClick = screenModel::toggleFavorite`).

Existing composables that still call a repository directly (e.g. `AddToListDialog`) are known debt
predating this rule, not a precedent to copy - migrate them opportunistically when touching that
file, and never add a new direct call while touching one.

## 7. A reusable composable never gets its own ViewModel (agreed 2026-08-16)

A `ViewModel` is scoped to a `ViewModelStoreOwner` - an Activity, Fragment, or nav destination -
i.e. a *screen*. Android's own architecture guidance is explicit that you don't pass `ViewModel`
instances down to composables, and by extension you don't construct a `ViewModel` (even via
`viewModel(key = "...:$id")`) *inside* a composable that isn't itself a screen and is reused across
several places (a hero action row, a list item, anything shared by more than one destination):
https://developer.android.com/develop/ui/compose/state-hoisting.

Two failure modes this rule exists to stop, both hit while building the favorite/watchlist icons:
- **A `ViewModel` per widget instance.** An earlier draft of `MediaActionButtons` built its own
  `viewModel(key = "MediaActionsScreenModel:$mediaType:$mediaId") { MediaActionsScreenModel(...) }`
  right inside the composable. That's a `ViewModel` for something that isn't a screen, keyed by
  data the widget happens to be showing - exactly the anti-pattern the guidance above warns about.
- **A composable triggering its own load.** Even after that state moved into a plain (non-
  `ViewModel`) holder, the composable itself was still deciding *when* to fetch, via
  `LaunchedEffect(mediaActionsState, session.sessionId) { mediaActionsState.load(...) }`. That is
  still business logic living in the render function - see §6.

The fix used throughout this codebase: business/action state that a reusable composable needs
(here, favorite/watchlist status) is a **plain class** (`MediaActionsState` - holds a
`StateFlow<UiState>` and mutation methods, nothing lifecycle-aware about it), constructed and
*owned* by the real screen-level `ViewModel` (`MovieDetailScreenModel.mediaActionsState`,
`TvDetailScreenModel.mediaActionsState`), running on that ViewModel's `viewModelScope` so its work
is cancelled with the screen, not with the widget's recomposition. The ViewModel also decides when
to call its `load()` (see §6). The screen threads the already-built holder down through its section
composables as a required parameter with no composable-level default.

## 8. A pure/reusable composable takes plain values and callbacks - never a ViewModel, a
   repository, or a `StateFlow` to collect (agreed 2026-08-16)

§7 stopped `MediaActionButtons` from owning a `ViewModel`. It is not enough by itself: the same
composable was still reading `mediaActionsState.uiState.collectAsState()` and calling
`mediaActionsState.toggleFavorite(...)` directly - i.e. it still held a reference to the
`ViewModel`-owned state holder, just not a `ViewModel` subclass. That is the same coupling with a
different type signature: the "pure" component still can't be previewed, tested, or reused without
dragging in a `MediaActionsState`/repository, and still decides *what a click does* instead of just
reporting that the click happened.

The rule: a presentational composable - anything not itself a screen - takes only what it renders
(`isFavorite: Boolean`, `isInWatchlist: Boolean`, ...) and a callback per action it can trigger
(`onFavoriteClick: () -> Unit`, `onWatchlistClick: () -> Unit`). It never takes a repository, a
`ViewModel`, a state holder like `MediaActionsState`, or anything it would call `.collectAsState()`
on. `MediaActionButtons` is the reference shape: zero imports from `features.account.repository` or
`features.auth.*`, nothing but `HeroColors`, booleans, and lambdas.

Something still has to bridge the screen's `ViewModel` to that pure component, and duplicating that
bridge in every caller is the duplication §2c exists to stop (`HeroActionRow` and `TvActionRow` both
need it identically). That bridge is its own composable, colocated with the pure one and named for
what it does (`MediaActionButtonsSection`, not `MediaActionButtons`) - it is the one place allowed
to hold the `AuthScreenModel`/`MediaActionsState` references, collect their state, and turn clicks
into ViewModel calls. Screens call the *section*, never the pure component, directly.
