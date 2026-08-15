# MyWatchList - Compose Multiplatform (Android / iOS / desktop / js)

## Mandatory conventions
Before writing or modifying Kotlin code, follow `.claude/skills/code-conventions/SKILL.md`:
- Catch/throw specific exception types only - never bare `Exception`.
- No magic strings: internal strings become `private const val`s; user-facing text goes in
  `composeApp/src/commonMain/composeResources/values/strings.xml` via `Res.string.*`.
- No wildcard imports - import every symbol explicitly.
- Check the platform first: before building a mechanism, find out whether Compose/Material3/Kotlin
  already provides it, and read the parameters of APIs already being called - the thing you need is
  often an argument that was there all along. Look the API up online against the versions in
  `gradle/libs.versions.toml` rather than trusting recalled knowledge, which is usually older than
  the versions here. Custom code is allowed but must say in its KDoc what the platform equivalent
  was and why it did not fit.

Before implementing any new feature, follow `.claude/skills/testing-conventions/SKILL.md`:
every new feature needs both a unit test (ScreenModel/repository logic) and a Compose UI JUnit
test (the composable's rendering and click-driven actions) - same tier as ktlint-clean, not
optional.

## Other project skills
- `.claude/skills/tmdb-api/SKILL.md` - how to look up TMDB endpoint schemas (OpenAPI) and
  ground-truth against the live API before adding/extending API models.
- `.claude/skills/run-app/SKILL.md` - how to run/screenshot each platform. Note: after code
  changes run tests + compile checks only; the user launches and verifies apps themselves.
- `.claude/skills/pr-screenshots/SKILL.md` - before/after screenshots for a visual PR, and how to
  host them without adding permanent weight to git. Read it *before* capturing: the order of
  operations is not recoverable if you rewrite history first.

## Verify
`./gradlew :composeApp:desktopTest` (tests), `:composeApp:compileKotlinDesktop` +
`:composeApp:assembleDebug` (compile checks), and `:composeApp:ktlintCheck` (lint - must be
zero violations before finalizing; `ktlintFormat` auto-fixes most).
