---
name: code-conventions
description: Coding conventions for this codebase - specific exception types only (never catch/throw bare Exception) and no magic strings (constants for internal strings, compose string resources for user-facing text). Apply to ALL new or modified Kotlin code.
---

# Code conventions (user-mandated)

Apply these to every piece of new or modified Kotlin code. Existing code migrates
opportunistically - when you touch a function, bring it up to standard.

## 1. No generic exceptions

Never catch or throw bare `Exception`/`Throwable`. Catch the specific failures a call can
actually produce. In this codebase the usual suspects are:

- `HttpExceptions` (project type, `network/exception/HttpExceptions.kt`) - non-2xx API responses
- `io.ktor.utils.io.errors.IOException` - connectivity failures
- `kotlinx.serialization.SerializationException` - malformed/unexpected payloads
- `io.ktor.client.plugins.HttpRequestTimeoutException` / `ConnectTimeoutException` - timeouts
- `IllegalArgumentException` - bad input parsing (dates, enums)

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
