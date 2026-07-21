# MyWatchList - Compose Multiplatform (Android / iOS / desktop / js)

## Mandatory conventions
Before writing or modifying Kotlin code, follow `.claude/skills/code-conventions/SKILL.md`:
- Catch/throw specific exception types only - never bare `Exception`.
- No magic strings: internal strings become `private const val`s; user-facing text goes in
  `composeApp/src/commonMain/composeResources/values/strings.xml` via `Res.string.*`.

## Other project skills
- `.claude/skills/tmdb-api/SKILL.md` - how to look up TMDB endpoint schemas (OpenAPI) and
  ground-truth against the live API before adding/extending API models.
- `.claude/skills/run-app/SKILL.md` - how to run/screenshot each platform. Note: after code
  changes run tests + compile checks only; the user launches and verifies apps themselves.

## Verify
`./gradlew :composeApp:desktopTest` (tests) and `:composeApp:compileKotlinDesktop` +
`:composeApp:assembleDebug` (compile checks).
