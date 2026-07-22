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
