# MyWatchList - Compose Multiplatform (Android / iOS / desktop / js)

## Mandatory conventions
Before writing or modifying Kotlin code, follow `.claude/skills/code-conventions/SKILL.md`:
- Catch/throw specific exception types only - never bare `Exception`.
- No magic strings: internal strings become `private const val`s; user-facing text goes in
  `composeApp/src/commonMain/composeResources/values/strings.xml` via `Res.string.*`.
- No wildcard imports - import every symbol explicitly.

Before implementing any new feature, follow `.claude/skills/testing-conventions/SKILL.md`:
every new feature needs both a unit test (ScreenModel/repository logic) and a Compose UI JUnit
test (the composable's rendering and click-driven actions) - same tier as ktlint-clean, not
optional.

## Other project skills
- `.claude/skills/tmdb-api/SKILL.md` - how to look up TMDB endpoint schemas (OpenAPI) and
  ground-truth against the live API before adding/extending API models.
- `.claude/skills/run-app/SKILL.md` - how to run/screenshot each platform. Note: after code
  changes run tests + compile checks only; the user launches and verifies apps themselves.

## Verify
`./gradlew :composeApp:desktopTest` (tests), `:composeApp:compileKotlinDesktop` +
`:composeApp:assembleDebug` (compile checks), and `:composeApp:ktlintCheck` (lint - must be
zero violations before finalizing; `ktlintFormat` auto-fixes most).
