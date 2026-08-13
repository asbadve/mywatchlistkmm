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
