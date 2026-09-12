---
name: release-process
description: How to cut a MyWatchList release - versioning, Android signing, DB migrations, ProGuard/R8, and the Play Store listing (copy, privacy policy, TMDB attribution). Apply before tagging a release, before any composeApp/build.gradle.kts signing/version change, before a MyDatabase.sq schema change on a branch heading toward release, and before touching the Play Store listing.
---

# Release process

Covers everything needed to cut a real, signed, versioned release across Android, macOS, Linux,
and Windows, and to keep the Play Store listing in step with it. iOS/JS are not part of the
automated release pipeline yet.

## Versioning - automatic, driven by the git tag

`composeApp/build.gradle.kts` reads `RELEASE_VERSION_NAME` from the environment (falls back to
`"1.0.0"` for ordinary local builds). `release.yml`'s `version` job computes it from the pushed
tag (`v1.2.3` -> `"1.2.3"`) and passes it to every packaging job. **Never hand-edit `versionCode`/
`versionName`/desktop `packageVersion` before a release** - tagging is the only bump needed.

- `versionCode` (Android, must strictly increase) = `major*1_000_000 + minor*1_000 + patch`.
- No tag (`workflow_dispatch`) falls back to `0.0.0-dev.<run number>` - useful for producing test
  artifacts without cutting a real release.

## Android signing - a real keystore, not the debug key

`composeApp/build.gradle.kts`'s `release` build type reads `ANDROID_RELEASE_KEYSTORE_PATH`/
`_KEYSTORE_PASSWORD`/`_KEY_ALIAS`/`_KEY_PASSWORD` from the environment. All four present -> real
signing; any missing -> falls back to the debug key (a warning fires on `assembleRelease`/
`bundleRelease`/`installRelease`) so the existing debug-signed-release benchmarking workflow (see
the detail-screen-scroll-jank skill) still works with no secrets exported.

CI sources these four from repo secrets: `ANDROID_RELEASE_KEYSTORE_BASE64` (the `.jks` file,
base64-encoded), `ANDROID_RELEASE_KEYSTORE_PASSWORD`, `ANDROID_RELEASE_KEY_ALIAS`,
`ANDROID_RELEASE_KEY_PASSWORD` - decoded to a runner-local temp file in `release.yml`'s
`package-android` job.

**The keystore itself is a real, hard-to-replace credential** - it was generated once
(2026-09-12, alias `mywatchlist`, valid until 2054) and lives outside the repo
(`~/keystores/mywatchlist-release.jks` on the machine that generated it, backed up by the
project's own account holder). Losing it means every future release needs a *new* signing
identity, and Play Store enforces the original key for updates to an already-published app -
there is no recovery path. Never regenerate it "to fix a build issue" without confirming with
the user first.

## Database migrations - real, not the debug wipe

`LocalSchemaVersion.CURRENT` (in `db/LocalSchemaVersion.kt`) tracks SQLDelight's own
`MyDatabase.Schema.version`. A schema-touching change to `MyDatabase.sq` (new table, new column a
query now selects, changed query shape) on any branch that will ship needs **both**:

1. A new numbered `N.sqm` migration file next to `MyDatabase.sq`, written to take a database on
   version `N-1` to exactly what `MyDatabase.sq` now describes.
2. `LocalSchemaVersion.CURRENT` bumped to match `N`.

Android/iOS apply this automatically (`AndroidSqliteDriver`/`NativeSqliteDriver` run
`create()`/`migrate()` against the versioned schema on their own). Desktop's `DatabaseDriverFactory.kt`
does it by hand against SQLite's `PRAGMA user_version` - if that file's migration logic is ever
touched, re-verify by creating an old-version on-disk file and confirming a fresh launch migrates
it rather than crashing or silently skipping.

`verifyMigrations.set(true)` in `composeApp/build.gradle.kts`'s `sqldelight` block is commented
out - it requires a generated schema-snapshot Gradle task this SQLDelight version's default task
graph doesn't produce with zero `.sqm` files present. **Turn it on when the first real `.sqm` file
is added** (and resolve whatever task-graph gap remains at that point) - don't ship a second
schema change without it.

The debug-only wipe-on-mismatch in `db/DatabaseDriverFactory.kt` (commonMain) still exists as a
dev-loop shortcut for a schema still in flux - it is not a substitute for the above on any branch
headed for release.

## ProGuard / R8

Enabled (`isMinifyEnabled`/`isShrinkResources` on the `release` build type,
`composeApp/proguard-rules.pro`). After adding a new dependency, adding a new
`@Serializable` model, or otherwise changing what reflection-based code touches:

1. `./gradlew :composeApp:assembleRelease` - a `Missing class`/`Missing rules` R8 error names the
   exact class and points at `build/outputs/mapping/release/missing_rules.txt` with a suggested
   rule.
2. Install the resulting APK on an emulator and click through the screens the new code affects -
   R8 stripping a class used only via reflection (a serializer, a service-loaded driver) doesn't
   always show up as a build error; it shows up as a runtime crash the build never caught.

## Play Store listing

- **Listing copy** (title, short/full description) lives in `docs/play-store-listing.md`
  (gitignored, local-only reference - same pattern as this project's other `docs/*.html`/`.md`
  planning files). Keep it in sync with the feature list in `future_features_checklist.md`.
- **TMDB attribution - required, not optional.** TMDB's API Terms of Use require the notice
  *"This product uses the TMDB API but is not endorsed or certified by TMDB."* to appear (a) in
  the store listing description and (b) somewhere in the app itself (their attribution logo or
  the same text). **Done 2026-09-12**: `AccountScreen`'s settings list ends with the exact
  required sentence plus a "Privacy Policy" row.
- **Privacy policy** - Play Console requires a URL, and it must be one the in-app consent gate
  (below) can actually load. **Hosted on GitHub Pages** (`gh-pages` branch, `privacy-policy.html`
  - a standalone static page, no framework dependency): https://asbadve.github.io/mywatchlistkmm/privacy-policy.html.
  **Do not host this as a Claude Artifact URL** - tried first and confirmed broken 2026-09-12: an
  artifact page's cross-origin frame-shell architecture (content loads through a `postMessage`
  handshake between `claude.ai` and a `claudeusercontent.com` sub-frame) doesn't complete inside
  an embedded `WebView`/`WKWebView`, only a full top-level browser tab - the page rendered fine
  when opened directly in Chrome but silently never loaded inside the app's own consent gate.
  Content covers: what's collected (TMDB OAuth session, locally-cached favorites/watchlist/lists/
  detail payloads - see `future_features_checklist.md` item 2 - and local notification state),
  that there's no separate analytics/tracking layer, that TMDB/JustWatch/Google Play are
  third-party data processors under their own terms, and the TMDB attribution notice.
- **First-launch consent gate - done 2026-09-12** (`core/ui/privacy/PrivacyConsentGate.kt`):
  blocks the app after splash until the user checks a box and taps Accept, persisted via
  `PrivacyConsentRepository` so it only shows once per install. Android/iOS render the policy in a
  real embedded `WebView`/`WKWebView` (JavaScript enabled - the page's back-to-top button needs
  it, and enabling it also happens to be what would have been required for a JS-dependent host
  like an Artifact URL); Desktop/JS get a native Compose summary with a system-browser link
  instead, since Compose Desktop has no built-in WebView and JCEF is too heavy to justify for a
  screen shown once (see this file's Android/iOS signing section for other JCEF-avoidance calls in
  this codebase). Both embedded-WebView platforms track load failures and offer a retry rather
  than leaving the user stuck on an infinite spinner with no way past an unskippable gate.
- **Screenshots** - capture fresh ones per the run-app skill once the listing's feature set is
  final; Play Console wants phone screenshots at minimum, tablet/10-inch optional.

## Cutting the release

1. Land all release-bound changes on `master` (a `release/*` branch + PR, same as any other
   change - see recent PR #11/#12 for the pattern).
2. `git checkout master && git pull`.
3. `git tag -a vX.Y.Z -m "vX.Y.Z" && git push origin vX.Y.Z` - this alone triggers `release.yml`:
   `version` -> `verify` -> `package` (macOS/Linux/Windows desktop) + `package-android` -> `release`
   (publishes a GitHub Release with all four artifacts attached, tag pushes only).
4. Separately: upload the Android artifact to Play Console (not automated - this skill's "Play
   Store listing" section covers what needs to be ready first).
