# Future Features Checklist (Based on TMDB OpenAPI Spec)

This document contains a checklist of high-priority features that can be implemented next, based on the endpoints defined in the TMDB OpenAPI Specification.

---

## Already shipped

Items 2-13 shipped and moved to [`shipped_features.md`](shipped_features.md) - kept in full, because the *why*
behind them (poller design, region resolution, the splash-icon and scroll-jank dead ends) is
what stops the same ground being re-covered. This file holds only what is still open.

| Feature | Status |
|---|---|
| [2. Local SQLite Database for Favorites/Watchlist (Local Notification Data Source)](shipped_features.md#2-local-sqlite-database-for-favoriteswatchlist-local-notification-data-source--done) | DONE |
| [3. Local Notifications (Returning Series, Favorite Actors, Favorite Collections)](shipped_features.md#3-local-notifications-returning-series-favorite-actors-favorite-collections) | DONE |
| [4. Adopt Material 3 Expressive](shipped_features.md#4-adopt-material-3-expressive--on-hold-waiting-on-compose-multiplatform-researched-2026-08-18) | ON HOLD |
| [5. Integrated Search Feature](shipped_features.md#5-integrated-search-feature--done-2026-08-04) | DONE |
| [6. Account Favorites & Watchlist (Replacing "My Fav" Placeholder)](shipped_features.md#6-account-favorites--watchlist-replacing-my-fav-placeholder--done-2026-08-16) | DONE |
| [7. Media Detailed Views (Movies & TV Shows)](shipped_features.md#7-media-detailed-views-movies--tv-shows--done-2026-08-16) | DONE |
| [8. Genre-based Discovery Screen](shipped_features.md#8-genre-based-discovery-screen--done-2026-08-18) | DONE |
| [9. TMDB User Authentication / Login](shipped_features.md#9-tmdb-user-authentication--login--done-2026-08-15) | DONE |
| [10. Restricted Mode Setting (Adult Content Toggle)](shipped_features.md#10-restricted-mode-setting-adult-content-toggle--done-2026-08-17) | DONE |
| [11. Region Selector Driving OTT Availability](shipped_features.md#11-region-selector-driving-ott-availability--done-2026-08-17) | DONE |
| [12. Animated Splash Screen](shipped_features.md#12-animated-splash-screen--done-2026-08-27) | DONE |
| [13. Small Increments (Quick Wins)](shipped_features.md#13-small-increments-quick-wins) | DONE |

**Still open inside those shipped items** - the only unchecked boxes left in the archive, listed
here so they are not lost behind the "DONE" markers above:

- [ ] Desktop notification banners do not deep-link ([item 3a](shipped_features.md#3a-returning-series---newupcoming-episode--done-2026-08-26)) -
  an AWT `TrayIcon` limitation, not this app's bug.
- [ ] Replace `java.awt.TrayIcon` with ComposeNativeTray ([item 3a](shipped_features.md#3a-returning-series---newupcoming-episode--done-2026-08-26)) -
  would fix the above *and* unlock desktop notification poster images.
- [ ] Optional cross-device sync for favorited people ([item 3b](shipped_features.md#3b-favorite-actorperson---new-credit-announced--done-2026-08-26)) -
  needs this app's own backend; deferred as a separate pass.

---

## 1. Secure the TMDB API Key via a Server-Side Proxy
**Goal**: Stop shipping the real TMDB API key inside the client at all. GitHub Actions Secrets
only keep it out of *CI logs* - it's still a plain string constant embedded in every shipped
binary (Android/desktop/iOS/JS), trivially extractable regardless of ProGuard obfuscation
(obfuscation renames code, it doesn't hide string constants). For an actual secret, the only real
fix is to keep the key server-side and proxy the call. With Firebase specifically:
- **Cloud Functions for Firebase** (or Cloud Run) — write a callable/HTTPS function that holds the
  real key as an environment variable/secret (Google Secret Manager), and the app calls that
  function instead of the third-party API directly. The function does the actual TMDB API call
  server-side.
- **App Check** — pair this with Cloud Functions to verify requests are genuinely coming from
  this app (not a scraped/tampered client or a bot hitting the proxy endpoint directly), closing
  the gap that Cloud Functions alone would leave open.

### Gateway options (pick one — the client-side work is identical either way)
The app only needs a base URL swap plus dropping the `api_key` parameter, so the hosting choice is
reversible. Compared on what actually differs for this app: cold-start latency on a
poster-grid-heavy UI, whether the platform can attest the caller, and cost at hobby volume.

| Option | Secret storage | Caller attestation | Notes |
|---|---|---|---|
| **Firebase Cloud Functions + App Check** | Google Secret Manager | Play Integrity (Android), DeviceCheck/App Attest (iOS), reCAPTCHA (web) | Best attestation story of the lot and the only one with first-party App Check. Cold starts are the worst here; needs the Blaze plan. |
| **Cloudflare Workers** | Worker secrets (`wrangler secret put`) | None built in — needs a self-issued token or Turnstile | Effectively no cold start and a generous free tier; also gives edge caching of TMDB responses, which this app would benefit from. No first-party mobile attestation. |
| **Supabase Edge Functions** | Supabase secrets / Vault | Supabase Auth JWT (needs a user or anon session) | Worth it only if TMDB user login (item 9) lands and its auth is wanted anyway; otherwise it's a lot of platform for one proxy. |
| **AWS API Gateway + Lambda** | Secrets Manager / Parameter Store | API Gateway usage plans, WAF, Cognito | The most configurable and the most YAML. Sensible only if there's already AWS in the picture. |
| **Self-hosted reverse proxy** (Caddy/nginx/Ktor) | Env file on the host | Whatever is built by hand | Cheapest at scale, but now there's a server to patch, monitor and keep online. |

Regardless of gateway, the proxy should also **rate-limit per caller and cache TMDB responses** —
otherwise the key stops leaking but the endpoint itself becomes the abusable resource, and TMDB's
rate limit applies to the proxy's key for every user at once. Note that no gateway makes the
endpoint fully private: without attestation anyone who extracts the proxy URL can call it, so the
threat model shifts from "key stolen forever" to "endpoint abusable, revocable, rate-limited".

### Implementation Checklist:
- [ ] **Server-side**:
  - Set up a Firebase project (or reuse an existing one) and enable Cloud Functions.
  - Write a callable/HTTPS function per TMDB endpoint family (or one generic pass-through
    function) that reads the real key from Google Secret Manager and forwards the request to
    TMDB, returning the response as-is.
  - Store the real TMDB key only in Secret Manager / the function's environment - never in the
    repo, never in the client.
- [ ] **App Check**:
  - Enable Firebase App Check for the project and configure the appropriate attestation provider
    per platform (Play Integrity for Android, DeviceCheck/App Attest for iOS, reCAPTCHA for web).
  - Require a valid App Check token on every call to the proxy function; reject requests without
    one.
- [ ] **Client**:
  - Replace direct TMDB network calls in the repositories with calls to the new proxy function
    (same request/response shapes where possible, to minimize changes elsewhere).
  - Remove `TMDB_API_KEY`/`MY_WATCH_LIST_TMDB_API_KEY` from the client build entirely once the
    proxy is live end-to-end (build.gradle.kts `buildConfigField`, GitHub Actions secret, local
    `~/.gradle/gradle.properties`).

---

## 14. AI-Powered "For You" Recommendations (Taste Profile from Favorites / Watchlist / Lists)
**Goal**: Generate personalized recommendations from what the user has *already curated in this
app* (their TMDB favorites, their watchlist, their favorited people, and the custom lists they
have built), instead of the per-title "more like this" TMDB already returns. TMDB's
`/3/movie/{id}/recommendations` and `/similar` are item-to-item: they answer "what is like *this
one* title" (already shipped, see movies' `RecommendationsSection.kt` and TV's
`TvRecommendationsSection.kt` on the detail screens). Nothing in the app currently answers "given
*everything* this user tracks, what should they watch next, and why" - which is the
recommendation users actually want on the home screen, and the one that can explain itself
("because you favorited three Denis Villeneuve films and have two slow sci-fi shows on your
watchlist").

### What this can build on (already in the app - no new data collection needed)
- `TrackedMediaRepository` (`features/account/repository/`) - the local SQLite `trackedMedia` table
  mirroring TMDB favorites *and* watchlist for the signed-in account, with `category`
  (`favorite`/`watchlist`) and media type already separated. This is the primary taste signal and
  it is readable offline, without re-hitting `/3/account/{account_id}/favorite/*` per request.
- `FavoritePersonRepository` (`features/person/repository/`) - the local-only `favoritePerson`
  table (TMDB has no account-level favorite API for people). Directors/actors the user follows are
  a strong, cheap taste signal.
- `CustomListRepository` / `ListsRepository` (`features/account/repository/`) - the user's TMDB
  custom lists (`customList`/`customListItem` tables). A hand-curated list is a *stronger* signal
  than a favorite, because the user chose both the members and the theme; the list *name*
  ("Comfort rewatches", "Heist movies") is itself usable as prompt context.
- `movieDetailCache` / `tvDetailCache` (`MyDatabase.sq`) - genres, keywords, cast/crew and
  overviews for titles the user has already opened, so building a taste profile does not require a
  fresh detail fetch per tracked item.
- The favorited-collection concept sketched in
  [item 3c](shipped_features.md#3c-new-movie-added-to-a-favorited-collection--done) - if that ships, followed franchises
  become another input (and a useful negative signal: don't recommend a franchise entry the user
  already tracks).

### Relevant OAS endpoints (for grounding + hydration, not for the ranking itself)
- `GET /3/search/movie`, `GET /3/search/tv` - resolve a model-proposed title (+ year) back to a
  real TMDB id. **Required**: an LLM must never be the source of a title that then gets rendered
  as if it came from TMDB.
- `GET /3/movie/{movie_id}` / `GET /3/tv/{series_id}` - hydrate a resolved id into the same
  `Movie`/`Tv` model the existing cards render, so recommendations reuse `MovieCard`/`MediaListRow`
  with no new UI model.
- `GET /3/discover/movie` / `GET /3/discover/tv` (already called by the genre-discovery screen,
  [item 8](shipped_features.md#8-genre-based-discovery-screen--done-2026-08-18)) - `with_genres`,
  `with_keywords`, `with_cast`, `with_crew`, `without_watch_providers`,
  `primary_release_date.gte`. The non-AI baseline *and* the candidate generator for the
  retrieval-then-rerank shape below.
- `GET /3/movie/{movie_id}/recommendations` & `/similar` (and the TV equivalents) - the other
  candidate source: seeds taken from the user's own favorites, pooled and de-duplicated.

### Architecture decision to make first: where does the model call happen?
Calling a hosted LLM from the client has exactly the API-key problem
[item 1](#1-secure-the-tmdb-api-key-via-a-server-side-proxy) already documents for the TMDB key -
except worse, because an LLM key is metered and directly billable to whoever extracts it from the
binary. **This feature should not ship before item 1's gateway exists**; the recommendation call
becomes a second route on that same Cloud Function / PocketBase / Cloud Run instance, and the app
ships no LLM credential at all.

Two shapes, in increasing cost/complexity - **start at the first**:
- [ ] **A. Retrieval, then LLM rerank + explain (recommended)**. The client (or the gateway)
  builds a candidate pool of ~50-100 titles from `/discover` + per-favorite `/recommendations`,
  filters out anything already in `trackedMedia`, and sends the model only a compact taste profile
  plus the candidate list (id + title + year + genres + a one-line overview). The model returns a
  ranked subset with a one-sentence reason each. Every returned id is validated against the
  candidate pool it was given, so a hallucinated title is structurally impossible and no extra
  TMDB lookup is needed. Cheap, fast, and degrades to "just show the candidate pool unranked" when
  the model call fails.
- [ ] **B. Free-form generation, then resolve**. The model proposes titles from its own knowledge;
  each is resolved via `/3/search/*`. More adventurous suggestions (it can reach titles TMDB's
  graph won't surface), but every result needs a search round-trip, unresolvable titles have to be
  dropped silently, and the model's training cutoff makes it weak exactly where users care most -
  new releases. Only worth attempting once A works and its recommendations feel too safe.

### Model / provider choice
- Provider: any hosted LLM behind the gateway; Anthropic's Claude API is the obvious fit given
  this repo's tooling. Cost as of 2026-09 runs roughly $1/$5 per million input/output tokens at
  the Haiku tier, $2/$10 at the Sonnet tier and $5/$25 at the Opus tier - verify against the
  provider's current pricing page before wiring billing, and look the request shape up against
  live API docs at implementation time rather than trusting recalled shapes (the same rule this
  repo already applies to TMDB and to Compose APIs - LLM API surfaces churn faster than either).
- Shape A's prompt is small (a taste profile plus ~100 one-line candidates, well under 10k input
  tokens), so the **cheapest tier is the right starting point** - reranking a supplied list is not
  a reasoning-heavy task. Measure quality before paying for a larger model.
- [ ] Use **structured outputs** (a schema of `{tmdbId, mediaType, reason}` objects) rather than
  parsing prose - the response goes straight into a `kotlinx.serialization` model, and a
  schema-invalid response becomes a typed failure instead of a regex.
- [ ] Prompt-cache the stable prefix (system prompt + instructions); keep the volatile part (taste
  profile + candidates) last, so repeat calls for the same user are cheap.

### Taste profile: what actually gets sent
Send an aggregated *profile*, not a raw dump of every tracked title - it is smaller, cheaper, and
sends far less about the user off-device:
- [ ] Top genres by frequency across favorites + lists, top favorited people (with their
  department, so "director" outranks "supporting actor"), most common keywords from the cached
  detail rows, decade distribution, and the user's average runtime.
- [ ] Weight favorites and custom-list members above watchlist items - a watchlist entry is an
  intention, a favorite is a verdict.
- [ ] Include custom-list *names* as themes, capped in count and length.
- [ ] Send the excluded-id set (everything in `trackedMedia`) as ids only, so the model never
  re-suggests something the user already tracks.
- [ ] Explicitly **not** sent: account id, session id, email, region, or anything from
  `AccountRepository` beyond the aggregate above.

### Implementation Checklist
- [ ] **Gateway route** (blocked on item 1): `POST /recommendations` holding the LLM key
  server-side, with the same App Check / attestation posture as the TMDB proxy route. Rate-limit
  per account - this is the one route in the app where a loop costs real money.
- [ ] **Data Layer**: `RecommendationRepository` /`RecommendationRepositoryImpl`
  (`features/recommendations/repository/`) - builds the taste profile from the repositories listed
  above, assembles the candidate pool, calls the gateway through the existing `TmdbClient`-style
  Ktor setup (its own client instance; different base URL and no `api_key` parameter), hydrates
  results, and caches the ranked output in a new `recommendationCache` table
  (`MyDatabase.sq`) keyed by account with a generated-at timestamp.
- [ ] **Refresh policy**: regenerate at most once a day, and on an explicit pull-to-refresh; also
  invalidate when the tracked set changes materially (a favorite added/removed), since a
  recommendation row that ignores a title the user just favorited reads as broken. Never call on
  every screen open.
- [ ] **Business Logic**: `RecommendationsScreenModel` - loading/success/empty/error states; an
  explicit **cold-start** state for a user with too few tracked items (say, under five) that
  shows trending instead of an empty shelf, and an explicit **signed-out** state (this feature
  needs an account, like
  [item 6](shipped_features.md#6-account-favorites--watchlist-replacing-my-fav-placeholder--done-2026-08-16)).
- [ ] **Failure handling**: a failed or slow model call must degrade to the unranked candidate pool,
  not to an error screen - the row still has real TMDB titles in it either way. Specific exception
  types only (per `.claude/skills/code-conventions/SKILL.md`) - no bare `Exception` around the
  gateway call.
- [ ] **UI Presentation**: a "For You" row on the home screen (reusing `MediaListRow`/`MovieCard`),
  each card carrying the model's one-line reason underneath, plus a "why this?" affordance showing
  the profile that produced it. A dedicated full-screen list behind "See all". All user-facing
  strings via `Res.string.*` in `composeResources/values/strings.xml`.
- [ ] **Transparency**: label the row as AI-generated, and keep a settings toggle
  (`AccountScreen`, alongside "Region" and restricted mode) that turns the feature off entirely -
  off means no profile is ever built or sent.
- [ ] **Tests** (both tiers required, per `.claude/skills/testing-conventions/SKILL.md`):
  - Unit: taste-profile aggregation (weighting, exclusion set, cold-start threshold), candidate
    filtering, the id-validation step that drops any model-returned id not in the candidate
    pool, and the degrade-to-unranked path on a gateway failure. The model call itself is
    stubbed - the tests must never hit a paid API.
  - Compose UI: the "For You" row renders titles + reasons, the cold-start and signed-out states
    render their fallbacks, and a card click navigates to the detail screen.
- [ ] **Verify**: `./gradlew :composeApp:desktopTest`, `:composeApp:compileKotlinDesktop`,
  `:composeApp:assembleDebug`, `:composeApp:ktlintCheck`.

### Deliberately out of scope here
- On-device / offline models. Nothing in the KMM ecosystem gives one runtime across Android, iOS,
  desktop *and* JS today, and the quality gap at phone-sized model scale isn't worth four
  platform-specific integrations for a "what should I watch" row.
- Training or fine-tuning anything on user data. The profile is assembled per request and thrown
  away; there is no model to train.
- Cross-device sync of the generated recommendations - same conclusion as item 3b's sync note: it
  would need this app's own backend, and regenerating locally is cheaper than syncing.
- Ratings as a signal. TMDB has `/3/account/{account_id}/rated/*`, but the app doesn't surface
  rating yet; add it as a profile input if/when rating ships.

### 14.1. Expose MyWatchList to on-device agents via Android AppFunctions
**Goal**: Turn the app into an on-device tool provider - the *inverse* of the rest of item 14.
Where 14 has this app call a model, AppFunctions
([developer.android.com/ai/appfunctions](https://developer.android.com/ai/appfunctions)) has the
system's agent call *this app*: annotated Kotlin functions are indexed by Android 16 and invoked by
an authorized assistant (Gemini, in Google's private preview) so "add Dune Part Two to my
watchlist", "what's on my watchlist that I can stream tonight", or "when's the next Reacher
episode" execute against this app's real data, with no chat UI built here and **no LLM key, no
gateway, and no per-request cost** - the model is the caller's, not ours. Google frames it as an
on-device MCP server: same tools-for-agents idea, but OS-level and local, so there is no network
round-trip and nothing to host.

**Status check before starting (as of 2026-09)**: experimental preview - `androidx.appfunctions`
is at `1.0.0-alpha11` (released 2026-08-26), execution requires **Android 16 (API 36)**, and the
Gemini integration is a private preview limited to trusted testers behind an Early Access Program
form. So this is prototype-grade: worth building to be first in line and to shake out the shape of
our own domain API, not something to put on a release's critical path. Expect breaking changes on
every alpha bump.

**Why this app is a good fit**
The app's verbs are already small, well-typed, and headless-callable - the hard part of adopting
AppFunctions is usually "our features only exist inside a ViewModel", and that is not the case
here. [Item 3](shipped_features.md#3-local-notifications-returning-series-favorite-actors-favorite-collections)
already proved the pattern: `EpisodeNotificationWorker` runs the pollers from a background worker
with no Activity, no Compose, and no DI container (repositories are constructed directly, and even
`Res.string.*` resolves via `getString` off the main thread). An `AppFunctionService` is the same
shape of caller. Most of the tracked data is also in local SQLite already
([item 2](shipped_features.md#2-local-sqlite-database-for-favoriteswatchlist-local-notification-data-source--done)), so
read functions answer offline and in milliseconds - exactly what an agent needs.

**Candidate functions to expose (start with reads)**

| Function | Backed by | Notes |
|---|---|---|
| `searchTitles(query, mediaType?)` | `SearchRepository` | The safest first function - read-only, no auth, already paginated. |
| `getWatchlist()` / `getFavorites()` | `TrackedMediaRepository` | Local SQLite, offline, no TMDB round-trip. |
| `getUpcomingEpisodes()` | `TrackedMediaRepository.trackedTvForPolling` + `TvRepository` | Reuses item 3a's existing poll state. |
| `whereToWatch(title)` | `MovieRepository`/`TvRepository` + `resolveRegion()` | Reuses the region resolution from [item 11](shipped_features.md#11-region-selector-driving-ott-availability--done-2026-08-17). |
| `addToWatchlist(title)` / `markFavorite(title)` | `AccountMediaRepository` | **Writes** - needs a signed-in TMDB session; see below. |
| `addToList(title, listName)` | `ListsRepository` | Write; resolve the list by name, don't create silently. |
| `recommendForMe(mood?)` | item 14's `RecommendationRepository` | The interesting bridge: hand the agent the *candidate pool + taste profile* and let **its** model do the ranking - shape A without paying for shape A. |

- [ ] Pick 2-3 read functions for the first cut. Resist exposing everything: each function is a
  permanent, agent-facing API contract, and a large surface makes the agent's tool selection worse,
  not better.

**Implementation checklist**
- [ ] **Build setup** (the real cost - none of this exists yet):
  - Add KSP to the build. Nothing in this project uses it today (SQLDelight has its own Gradle
    plugin), and in a KMP module the processor must be attached to the Android target only -
    `add("kspAndroid", libs.androidx.appfunctions.compiler)`, not a bare `ksp(...)`, or the other
    targets fail to configure.
  - `implementation(libs.androidx.appfunctions)` in `androidMain` only; `appfunctions-testing` in
    the Android test source set.
  - `compileSdk` is already 37 ✔. `targetSdk` is **34** and needs to reach 36 for the OS to index
    the functions - that is an app-wide change with its own behaviour-change review, not a
    one-liner, so scope it as a separate step.
  - `minSdk` stays 24: every AppFunctions entry point is `@RequiresApi(36)`, and the service
    declaration must be inert (never crash, never advertise) on older devices.
- [ ] **Service entry point** (`androidMain`, e.g. `core/appfunctions/`): an
  `@AppFunctionServiceEntryPoint`-annotated `AppFunctionService` subclass. The generated XML
  schema and the `EXECUTE_APP_FUNCTIONS`-gated service registration go in
  `composeApp/src/androidMain/AndroidManifest.xml`.
- [ ] **Function layer**: thin `@AppFunction suspend` wrappers that construct the same repositories
  `EpisodeNotificationWorker` does and delegate straight into `commonMain`. No business logic in
  `androidMain` - if a wrapper needs logic (title→id resolution, ranking, filtering), that logic
  belongs in a common-code function both the wrapper and the UI can call.
- [ ] **Agent-facing DTOs**: new `@AppFunctionSerializable` data classes in `androidMain` - do
  **not** annotate the existing `Movie`/`Tv`/`SearchResultItem` models. Those are TMDB wire shapes
  in `commonMain` (`kotlinx.serialization`, wrong module, wrong stability guarantee); the agent
  contract should be a deliberately small projection (id, title, year, mediaType, posterUrl).
- [ ] **KDoc is the API**: `@AppFunction(isDescribedByKDoc = true)` means the KDoc *is* what the
  agent reads to choose and fill the function - parameter meaning, units, what "title" accepts, and
  what the function will refuse. This is the one place in the repo where vague KDoc is a functional
  bug, not a style issue. Google ships an
  [AppFunctions agent skill](https://github.com/android/skills/tree/main/device-ai/appfunctions)
  for refining these, and a [sample app](https://github.com/android/appfunctions).
- [ ] **Auth and writes**: every write function checks the TMDB session first and throws a specific
  typed failure (`AppFunctionInvalidArgumentException` / a signed-out equivalent) rather than
  silently no-op'ing - an agent reporting "added it" when nothing was added is the worst possible
  outcome. Ambiguous title matches must fail loudly too: never guess between two results, return
  the candidates and let the agent disambiguate. Specific exception types only, per
  `.claude/skills/code-conventions/SKILL.md`.
- [ ] **User control**: a settings toggle on `AccountScreen` (next to "Region" and restricted mode)
  that disables the functions - reads and writes separately if the framework allows advertising
  them independently. Restricted mode must be honoured inside `searchTitles`, not just in the UI:
  an agent-driven search is still this app's search.
- [ ] **Tests**: unit-test the wrapper layer (title resolution, the signed-out and ambiguous-match
  failures, DTO projection) with repositories faked - these are Android-source-set tests, so they
  run under `:composeApp:testDebugUnitTest`, not `desktopTest`. The Compose UI test that
  `.claude/skills/testing-conventions/SKILL.md` mandates applies to the settings toggle (the only
  UI this adds); the functions themselves have no composable to test. Manual verification is
  `adb shell cmd app_function list-app-functions` against an API 36 emulator, per the docs.
- [ ] **Verify**: `./gradlew :composeApp:desktopTest`, `:composeApp:testDebugUnitTest`,
  `:composeApp:compileKotlinDesktop`, `:composeApp:assembleDebug`, `:composeApp:ktlintCheck` -
  and confirm the desktop/iOS/JS targets still configure after KSP lands, since that is the most
  likely thing this breaks.

**Risks / open questions**
- **Alpha churn.** `1.0.0-alpha11` in an experimental preview; assume each bump costs a small
  migration. Keep the surface small so those migrations stay cheap.
- **Gemini access is gated.** Without EAP admission the functions are only reachable via `adb`, so
  the honest deliverable of a first pass is "verified callable on-device", not "works in the
  assistant".
- **New attack surface.** A system-invocable service that can mutate the user's TMDB account is a
  bigger deal than anything the app ships today. Writes stay behind the session check *and* the
  settings toggle, and no function should ever expose the session id, account id, or API key.
- **Android-only, in a multiplatform app.** iOS's equivalent is App Intents - written up as its
  own sibling item in [14.2](#142-expose-mywatchlist-to-siri--apple-intelligence-via-ios-app-intents),
  since the two frameworks differ enough that neither is a port of the other.

### 14.2. Expose MyWatchList to Siri / Apple Intelligence via iOS App Intents
**Goal**: The iOS half of
[14.1](#141-expose-mywatchlist-to-on-device-agents-via-android-appfunctions) - same idea (the
system's assistant calls this app, so there is no LLM key and no per-request cost), different
framework. Apple's is
[App Intents](https://developer.apple.com/documentation/appintents): actions declared in Swift that
Siri, Apple Intelligence, Spotlight and the Shortcuts app can invoke - "add Dune Part Two to my
watchlist", "search MyWatchList for Villeneuve", or a Shortcut the user wires up themselves.

**This is not a port of 14.1.** The two frameworks are shaped differently enough that the design
has to be redone rather than translated:

| | Android AppFunctions | iOS App Intents |
|---|---|---|
| What you may expose | any function you annotate; KDoc is the contract | a **fixed catalogue of system schemas**, plus custom intents outside it |
| Maturity | `1.0.0-alpha11`, experimental preview | framework GA since iOS 16; Apple-Intelligence schemas newer (see below) |
| Caller | Gemini, gated behind an EAP | Siri, Apple Intelligence, Spotlight, Shortcuts - Shortcuts needs no special access |
| Language / location | Kotlin, `composeApp/src/androidMain` | **Swift, `iosApp/`** - intents cannot be written in Kotlin |
| Build cost here | KSP is new to the build; `targetSdk` 34 → 36 | none: deployment target is already 16.2, and App Intents is iOS 16+ |

**The catalogue problem (read this before scoping anything)**
Apple's schema domains are a closed list - audio, calendar, camera, clock, files, mail, maps,
messages, notes, phone, photos, reminders, and system-and-in-app-search as the primary ones, plus
Shortcuts-only domains (books, browser, journaling, reader, presentation, spreadsheet, whiteboard,
word processor) and two single-purpose ones (assistant, visual intelligence). **There is no media,
video, or watchlist domain**, so "add this film to my watchlist" has no schema to conform to. What
this app can actually claim from the catalogue is the search/open pair:
- `@AppIntent(schema: .system.searchInApp)` - conforms to `ShowInAppSearchResultsIntent`, with
  `searchScopes` and a `criteria: StringSearchCriteria`, and navigates the app to its search
  results. Note the version churn: `.system.search` arrived in iOS 18 and is **deprecated as of
  iOS 27** in favour of `.system.searchInApp`, which is itself iOS 27 and still in beta - so this
  one needs an availability-gated pair, not a single call site.
- `@AppIntent(schema: .system.open)` (iOS 27, beta) - opens a given `AppEntity`, i.e. deep-link
  into a movie/show detail screen.

Everything else this app would want to offer (mark favorite, add to watchlist, add to a list,
"what's airing this week") has no schema and stays a **plain custom `AppIntent`**. That is not a
failure mode - custom intents still power Shortcuts, the Action button, and Spotlight - it just
means Siri won't freely paraphrase them the way it does schema-backed actions.

**Staged plan - each stage ships value on its own**
- [ ] **Stage 1: custom intents + `AppShortcutsProvider`** (works on today's deployment target,
  every device, no Apple Intelligence). `SearchWatchlistIntent`, `AddToWatchlistIntent`,
  `MarkFavoriteIntent`, `UpcomingEpisodesIntent`, each with `AppShortcutPhrases` so they appear in
  Shortcuts and Spotlight. This is the honest first deliverable.
- [ ] **Stage 2: `AppEntity` + `IndexedEntity`** (iOS 18+, GA) for tracked movies/shows, so the
  user's own watchlist lands in the Spotlight index and becomes referenceable content rather than
  just a set of verbs.
- [ ] **Stage 3: schema conformance** (`.system.searchInApp` / `.system.open`, iOS 27, beta) behind
  `@available`, for the Apple Intelligence path. Gate this on the betas settling.

**Implementation checklist**
- [ ] **Where the code lives**: Swift files in `iosApp/iosApp/` added to the existing app target -
  no App Intents extension. An extension would need its own link against the `ComposeApp`
  framework, which is built `isStatic = true` (`composeApp/build.gradle.kts`), and duplicating a
  static framework across two targets is exactly the kind of build problem not worth taking on for
  a first pass. Adding files means editing the checked-in `project.pbxproj` by hand or via Xcode.
- [ ] **Bridging to shared code**: intents call `commonMain` repositories through the generated
  `ComposeApp` Objective-C interface. Kotlin `suspend` functions surface as completion-handler
  methods that Swift can `await`, so an intent's `async perform()` maps cleanly. Only `public`
  Kotlin declarations are exported - some repository methods may need widening, and top-level
  functions arrive as `<File>Kt` members.
- [ ] **Headless-launch safety**: an intent can run with no UI ever created, which is the same
  situation `NotificationScheduler`'s `BGTaskScheduler` handler already runs in - and that one
  needed `ensureRegisteredAtLaunch()` precisely because a Kotlin `object`'s lazy `init` fired at
  the wrong moment. Assume the same class of bug here: anything an intent touches
  (`AppDatabaseProvider`, the Ktor client, settings) must be safe to initialize outside
  `MainViewController()`, and that needs proving on device, not reasoning about.
- [ ] **Result shapes**: return `IntentResult` with a dialog **and** a snippet view for anything a
  user would want to see (the upcoming-episodes list, search results). A Siri response that is
  text-only for a poster-driven app is a missed opportunity, and snippet views are plain SwiftUI.
- [ ] **Auth and writes**: same rules as 14.1 - check the TMDB session before any write, throw a
  specific error rather than silently no-op'ing, and never disambiguate a title by guessing. Swift
  side, that means `throw` with a localized failure so Siri can speak it, and
  `requestDisambiguation` / `needsValueError` where the framework can ask the user instead.
- [ ] **Localization**: intent titles, parameter prompts and phrases live in Apple's own
  `AppShortcuts.strings`/string catalogs, **not** in `composeResources/values/strings.xml`. This is
  the one deliberate exception to the repo's "all user-facing strings via `Res.string.*`" rule, and
  it should be called out in the code's KDoc/comments so it doesn't read as an oversight.
- [ ] **User control**: honour the same settings toggle 14.1 adds (shared preference in
  `commonMain`, read by both platforms), and honour restricted mode inside the search intent.
- [ ] **Tests**: the intents themselves are Swift and outside the Gradle test tiers - unit-test the
  shared logic they call (title resolution, session checks) in `commonTest` so it is covered once
  for both platforms, and verify the intents by hand in the Shortcuts app plus Siri on a device.
  Note in the PR that `.claude/skills/testing-conventions/SKILL.md`'s Compose UI test requirement
  is satisfied by the settings toggle, since App Intents add no composable.
- [ ] **Verify**: `./gradlew :composeApp:desktopTest`, `:composeApp:compileKotlinDesktop`,
  `:composeApp:ktlintCheck`, plus an Xcode build of `iosApp` (the Gradle tasks alone will not catch
  a Swift or pbxproj mistake).

**Risks / open questions**
- **iOS 27 is beta.** Both schemas this app can use are beta at the time of writing, and
  `.system.search` was deprecated one version after arriving - so stage 3 carries real churn risk
  while stages 1 and 2 do not.
- **Apple Intelligence is device-gated** (supported hardware only) and region/language-gated;
  Shortcuts and Spotlight are not. Framing this feature as "Siri support" would overpromise for
  most of the install base - "Shortcuts and Spotlight, with Siri where available" is accurate.
- **No schema for what this app mostly does.** Worth periodically re-checking the domain list: if
  Apple ever ships a media/watchlist domain, the custom intents from stage 1 should be re-shaped to
  conform to it, and that would be a breaking change to any Shortcut users have built.
- **`project.pbxproj` churn.** The Xcode project is checked in; adding a Swift file touches a
  generated-looking file that merges badly. Keep the intent files in one group, added in one commit.

---

## 15. Backup & Restore for Device-Only Data (the data TMDB does not hold)
**Goal**: Give the user a file they own for everything this app keeps *only on their device*.
Favorites, watchlist and custom lists all come back by themselves on the next sign-in - they're
mirrors of TMDB account state ([item 2](shipped_features.md#2-local-sqlite-database-for-favoriteswatchlist-local-notification-data-source--done),
[item 6](shipped_features.md#6-account-favorites--watchlist-replacing-my-fav-placeholder--done-2026-08-16)) - but the
local-only concepts have no server behind them at all. Followed people
([item 3b](shipped_features.md#3b-favorite-actorperson---new-credit-announced--done-2026-08-26)) and followed
collections ([item 3c](shipped_features.md#3c-new-movie-added-to-a-favorited-collection--done)) exist *because* TMDB
has no account API for them, and the settings that shape the whole app (region, restricted mode,
notification opt-in, Discover filters) live in `multiplatform-settings`. Reinstall the app, move to
a new phone, or hit "Clear data" and every one of those is gone with nothing to sync back from.

**No TMDB endpoints are involved - that is the entire point of this item.** Unlike every other
entry in this file, there is no OAS section here: the data being backed up is precisely the data
TMDB will never return.

### Exact scope: what goes in the file
Decided by "would this come back on its own after a reinstall?" - if yes, it stays out.

| Data | Where it lives now | Comes back on its own? | In the backup |
|---|---|---|---|
| Followed people | `favoritePerson` table, `FavoritePersonRepository` | **No** - TMDB has no favorite/follow API for people (confirmed 2026-08-26, see the table's kdoc) | ✅ |
| Followed collections | `favoriteCollection` table, `FavoriteCollectionRepository` | **No** - same finding for collections | ✅ |
| Selected + fallback region | `region_selected_code` / `region_fallback_code` (`RegionConstant`) | No | ✅ |
| Restricted mode | `restricted_mode_enabled` (`RestrictedModeConstant`) | No | ✅ |
| Episode-notification opt-in | `episode_notifications_enabled`, `episode_alert_opt_in_prompt_seen` (`NotificationSettingsConstant`) | No | ✅ |
| Saved Discover filters | `discover_movie_filters_json` / `discover_tv_filters_json` (`DiscoverFilterRepository`) | No | ✅ |
| Favorites / watchlist / custom lists | `trackedMedia`, `customList`, `customListItem` | **Yes** - re-synced from TMDB on sign-in | ❌ |
| Detail caches, `remoteKeys` | `movieDetailCache` / `tvDetailCache` / `tvSeasonDetailCache` / `personDetailCache` / `remoteKeys` | Yes - pure read-through caches | ❌ |
| Poll state + notification ledger | `trackedMedia.lastKnown*`, `favoritePerson.lastKnownCreditIds`, `favoriteCollection.lastKnownPartIds`, `notificationLedger` | n/a - see below | ❌ (deliberate) |
| TMDB session | `auth_session_id`, `auth_account_id`, `auth_username`, `auth_name`, `auth_avatar_url` (`AuthRepository`) | Yes - the user signs in again | ❌ **never** |
| Privacy-policy acceptance | `privacy_policy_accepted_v1` (`PrivacyConsentRepository`) | n/a - re-shown per install | ❌ (deliberate) |

If [item 16](#16-release-date-reminders-for-unreleased-titles-remind-me-cta--day-before--release-day-alerts)
ships, its local-only `releaseReminder` table joins the ✅ rows above by the same test (its
`lastKnownReleaseDate` stays out, like the other poll-state columns).

Three exclusions are decisions, not oversights, and each should be stated in the exporter's KDoc so
nobody "fixes" them later:
- **The auth session must never be written to the file.** `auth_session_id` is a bearer credential
  for the user's real TMDB account; a backup file gets copied into cloud drives, chat apps and
  email. Export works by an explicit allow-list of keys, never by dumping the whole `Settings`
  store - that way a future settings key can't silently join the export.
- **Poll state stays out, and that is what makes restore quiet.** `lastKnownCreditIds` /
  `lastKnownPartIds` being `NULL` already means "never polled yet", and
  `PersonCreditNotificationPoller` / `CollectionNotificationPoller` treat that first poll as
  baselining only (see both tables' kdocs). Restoring 40 followed people with no poll state
  therefore fires **zero** notifications on the next poll, instead of one per past credit. Restore
  gets the right behaviour by writing *less*, not more.
- **The privacy-consent flag stays out.** Restoring an "already accepted" boolean from a
  user-editable file would let a fresh install skip its consent gate.

### Format: one versioned JSON file, not a copy of the database
- [ ] A single `.json` file, `kotlinx.serialization` (the app's only serializer already), named
  `mywatchlist-backup-YYYY-MM-DD.json`. The payload is at most a few hundred short rows, so a
  human-readable, diff-able text file costs nothing.
- [ ] Envelope: `{ "format": 1, "exportedAt": <epochMillis>, "people": [...], "collections": [...],
  "settings": { ... } }`. `format` is the **backup contract's** own integer and is deliberately
  *not* `LocalSchemaVersion.CURRENT` - a `MyDatabase.sq` migration that doesn't change what's
  exported must not invalidate old backups, and a change to the exported shape must bump something
  even when the schema is untouched. Say exactly this in its KDoc, since the two look
  interchangeable at a glance.
- [ ] Decode with `Json { ignoreUnknownKeys = true }` (the same setting every other decode in this
  app uses), so a file written by a newer build restores its known parts on an older one instead of
  failing outright. A `format` *greater* than the build knows is still refused explicitly, with a
  typed result - silently half-restoring a future format is worse than declining.
- **Not** the SQLite file itself: copying it would drag in the TMDB-derived caches this item just
  excluded, tie every backup to the SQLDelight schema version and its migration chain, and hand the
  user an opaque blob instead of something they can read.

### Restore semantics (decide these before writing code)
- [ ] **Merge by default, never wipe-and-replace.** A restore adds the people/collections that
  aren't already followed and leaves existing ones alone. Both repositories' `setFavorite` is
  already backed by `INSERT OR REPLACE`, so merge is the cheap path; a destructive "replace
  everything" mode is the one that would need extra code, and it isn't worth shipping in the first
  cut.
- [ ] **Preserve `addedAt` from the file.** `selectAllFavoritePeople` / `selectAllFavoriteCollections`
  order by `addedAt DESC` ("most-recently-followed first"), so restoring with `now()` would scramble
  the user's ordering into "whatever order the JSON array happened to be in". Today's
  `setFavorite(...)` signature takes no `addedAt`, so this needs a new repository method (e.g.
  `restoreFavoritePeople(List<BackupPerson>)`) rather than the backup layer reaching past the
  repositories into SQLDelight directly.
- [ ] **Settings restore is per-key and only for keys present in the file** - a backup written
  before a setting existed must not reset that setting to a default.
- [ ] Restore never touches `trackedMedia` / `customList` / `customListItem` / the caches.

### The real cost: this app has no file picker on any platform
Checked before designing anything, per `.claude/skills/code-conventions/SKILL.md`'s "check the
platform first": **Compose Multiplatform 1.11.1 (`gradle/libs.versions.toml`) ships no common file
dialog** - confirmed 2026-09, it has never had one. The closest thing already in the repo is
`util/ImageSaver.kt`, an `expect class` with four actuals, and it does not fit: it *writes* only, to
a fixed location per platform (Android MediaStore/Pictures, iOS Photos album, desktop `~/Downloads`,
a JS anchor-click download), with no user-chosen destination and no read counterpart at all. Restore
needs a *read* from a user-picked file, which is genuinely new on all four targets.

Two ways to get it - **pick one before starting**:
- **A. Hand-rolled `expect`/`actual`, no new dependency (recommended).** Android:
  `ActivityResultContracts.CreateDocument` / `OpenDocument` (SAF - no storage permission needed).
  iOS: `UIDocumentPickerViewController`. Desktop: `java.awt.FileDialog` via `AwtWindow`. JS: the
  existing Blob-download trick for export plus a hidden `<input type="file">` for import. Android
  and iOS need a live Activity/`UIViewController`, which is exactly the problem
  `rememberWebAuthLauncher()` (`core/auth/WebAuthLauncher.kt`) and
  `rememberNotificationPermissionRequester()` already solved in this repo as a
  `@Composable expect fun remember…()` returning an interface - copy that shape rather than
  inventing a third one. Cost: four small platform files, no dependency, full control.
- **B. FileKit (`io.github.vinceglb:filekit`).** One common API covering Android/iOS/JVM/JS
  (`FileKit.openFilePicker()` / `saveFile()`), backed by the same native pickers option A would call
  by hand. **Check compatibility first**: 0.16.0 (released ~Sept 2026) is built against Compose
  Multiplatform **1.12**, while this repo is on **1.11.1**, so this is a dependency bump decision,
  not a drop-in. It would also be this app's first third-party dependency for platform IO.
- Whichever wins, the custom code's KDoc must record what the platform equivalent was and why it
  didn't fit (the convention's requirement) - here: "CMP has no common file dialog as of 1.11.1".

### Implementation Checklist
- [ ] **Models** (`features/settings/model/Backup.kt`): `@Serializable` `BackupEnvelope`,
  `BackupPerson(id, name, profilePath, addedAt)`, `BackupCollection(id, name, posterPath, addedAt)`,
  `BackupSettings(...)` - one nullable field per allow-listed key, so "absent" and "set to false"
  stay distinguishable.
- [ ] **Repository** (`features/settings/repository/BackupRepository.kt` + `Impl`):
  `suspend fun exportToJson(): String` and `suspend fun restoreFromJson(json: String): RestoreResult`.
  It composes the existing repositories (`FavoritePersonRepository`, `FavoriteCollectionRepository`,
  `Settings`) - no raw SQLDelight access, same layering every other repository here follows.
- [ ] **Typed outcomes, no bare `Exception`** (per `.claude/skills/code-conventions/SKILL.md`): a
  `sealed interface RestoreResult` with `Restored(peopleCount, collectionsCount, settingsCount)`,
  `UnsupportedFormat(found, supported)` and `Malformed`. Catch `SerializationException` and
  `IllegalArgumentException` specifically - the exact pair `DiscoverFilterRepositoryImpl` already
  catches around its own JSON decode.
- [ ] **Platform IO**: the picker/writer chosen above, plus an in-memory fake for tests.
- [ ] **Business logic**: `BackupScreenModel` - idle / exporting / restoring / result states, and an
  explicit confirm step before a restore runs.
- [ ] **UI**: a "Backup & restore" row on `AccountScreen`
  (`features/auth/screen/AccountScreen.kt`), alongside the existing Region / Restricted mode /
  Privacy policy rows, opening a small screen or dialog with "Export backup" and "Restore from
  file". Result reporting goes in a dialog or inline text, **not** a snackbar - this app has no
  snackbar host anywhere (established in [item 13.2](shipped_features.md#132-imdb-link-on-every-detail-screen--long-press-to-copy-on-detail-titles--done)),
  and adding one app-wide is out of scope here.
- [ ] **Strings**: every user-facing string via `Res.string.*` in
  `composeApp/src/commonMain/composeResources/values/strings.xml` (`settings_backup_label`,
  `backup_export_action`, `backup_restore_action`, `backup_restore_confirm_message`,
  `backup_restore_result_message`, `backup_error_unsupported_format`, …). No magic strings; the
  settings keys the exporter allow-lists are `private const val`s referencing the existing
  `*Constant` objects, not re-typed literals.
- [ ] **Tests** (both tiers required, per `.claude/skills/testing-conventions/SKILL.md`):
  - Unit: export→restore round-trip preserves people, collections and their `addedAt` ordering;
    merge keeps pre-existing follows; a `format` from the future returns `UnsupportedFormat`;
    malformed JSON returns `Malformed` and writes nothing; unknown extra fields decode fine; and -
    the one that matters most - **the exported JSON contains no `auth_` key and no session id**, as
    a standing regression test for the leak this design exists to prevent.
  - Compose UI: the Account row renders and opens the screen, the restore confirm dialog appears and
    its confirm action calls through, and the error result renders its message.
- [ ] **Verify**: `./gradlew :composeApp:desktopTest`, `:composeApp:compileKotlinDesktop`,
  `:composeApp:assembleDebug`, `:composeApp:ktlintCheck`.

### Deliberately out of scope
- **Cloud sync / cross-device backup.** Same conclusion as
  [item 3b](shipped_features.md#3b-favorite-actorperson---new-credit-announced--done-2026-08-26)'s sync note and
  [item 14](#14-ai-powered-for-you-recommendations-taste-profile-from-favorites--watchlist--lists)'s:
  it needs this app's own backend. A file the user moves themselves needs none.
- **Android Auto Backup / iCloud key-value store.** Platform-specific, invisible to the user, and
  neither helps someone moving between Android and iOS - which is the case a KMM app should handle
  best, not worst.
- **Encryption / password-protecting the file.** Nothing in it is a credential, by construction. If
  that ever stops being true the answer is encryption, not a comment - so the allow-list is the
  thing to defend in review.
- **Scheduled or automatic backups.** Export is a user action. A background writer would need a
  destination it can write unattended, which is the one thing SAF/`UIDocumentPicker` deliberately
  don't give.

### Risks / open questions
- **JS barely has anything to back up.** Its SQLDelight driver is in-memory per page load
  (`jsMain/db/DatabaseDriverFactory.kt` - never persisted to IndexedDB/OPFS), so on web an export
  captures only the current session's follows plus the `Settings`-backed preferences. Either ship it
  there as-is with that caveat, or hide the row on JS - decide, don't leave it accidental.
- **File-size ceiling is not a concern, file *trust* is.** The JSON is user-editable by design, so
  the restore path validates every field (ids positive, names non-blank, region codes matching the
  existing region list) rather than trusting the file - a hand-edited backup should fail cleanly,
  never write junk rows.
- **The picker decision drives the effort estimate.** Option A is four small platform files;
  option B is a Compose Multiplatform version bump. Settle that first - everything else in this item
  is small.

---

## 16. Release-Date Reminders for Unreleased Titles ("Remind me" CTA + day-before / release-day alerts)
**Goal**: Let the user tap **"Remind me"** on a movie or show that hasn't come out yet, and get two
local notifications: one the day before it releases, one on release day. The app already *tells*
the user a title is unreleased - `UpcomingBadge` renders wherever `isUpcoming(today)` is true
(`SearchResultItem`/`Movie`/`Tv`, shown on the Discover tabs, search results and the favorites/
watchlist grid) - but then does nothing about it, so remembering to come back is entirely the
user's problem.

Nothing in [item 3](shipped_features.md#3-local-notifications-returning-series-favorite-actors-favorite-collections)
covers this. `TvEpisodeNotificationPoller` keys off `next_episode_to_air`, which an unpremiered
show doesn't have; `PersonCreditNotificationPoller`/`CollectionNotificationPoller` fire on *new*
credits/parts appearing, not on a known date arriving. And movies are polled by nothing at all -
`selectTrackedTvForPolling` is `WHERE mediaType = 'tv'`. An upcoming title falls through every
existing poller.

### What this reuses (most of it is already built)
- **The entire notification stack**, from [item 3](shipped_features.md#3-local-notifications-returning-series-favorite-actors-favorite-collections)'s
  shared infrastructure: `NotificationScheduler` (WorkManager / `BGTaskScheduler` / JVM executor /
  `setInterval`), `LocalNotifier`, `NotificationPermissionRequester`, `NotificationImageFetcher`
  for the poster, and `PendingNotificationTarget` for tap-to-open. **No new per-platform code.**
- **The 6-hour poll cadence is already the right one.** `AndroidNotificationSchedulerConstant.POLL_INTERVAL`
  is `6.hours`, so a day-granularity reminder gets four chances to fire. "Notify on the date
  itself" is also not a new shape - it is exactly what `NotificationReason.EPISODE_AIRING` already
  does by comparing an air date against `today()`.
- **The dedup ledger needs no change.** `notificationLedger` is keyed
  `(id, mediaType, reason, cursorValue)`; putting the *resolved release date* in `cursorValue`
  means a date that slips re-notifies for the new date while an exact repeat stays suppressed -
  the same property that lets `EPISODE_ANNOUNCED` fire again for a later season.
- **`release_dates` is already on the wire and already modeled.** `MovieRepositoryImpl`'s
  `append_to_response` includes `release_dates`, and `ReleaseDatesResponse` / `ReleaseDatesResult` /
  `ReleaseDateItem` all exist in `MovieDetail.kt` - including `type: Int`, which **nothing currently
  reads** (`usCertification()` is the only consumer and it only looks at `certification`). So for
  movies this feature needs no new endpoint, no new append value and no new model field.
- **Region resolution** from [item 11](shipped_features.md#11-region-selector-driving-ott-availability--done-2026-08-17):
  `RegionRepository` plus `resolveRegion()`/`resolveRegionCode()`'s selected → fallback → any
  priority.

### Relevant OAS endpoints
- `GET /3/movie/{movie_id}?append_to_response=release_dates` - **already called exactly like this.**
  `release_date` (the primary date) plus the per-country `release_dates.results[]` buckets.
- `GET /3/tv/{series_id}` - `first_air_date` and `status` (`"Planned"` / `"In Production"` /
  `"Returning Series"`), already fetched by `TvRepositoryImpl`.

### Decide first: *which* release date the reminder fires on
This is the one genuine design question, and getting it wrong makes the feature actively annoying.
- `movie.release_date` is TMDB's **primary** release date, which is frequently a festival premiere
  or a US theatrical date - not the date the title becomes available to this user.
- The `release_dates` buckets carry a `type`, documented by TMDB as: **1** Premiere, **2**
  Theatrical (limited), **3** Theatrical, **4** Digital, **5** Physical, **6** TV.
- [ ] **Ground-truth this against the live API before building**, per
  `.claude/skills/tmdb-api/SKILL.md` - the session that wrote this item could not (the sandbox
  blocks `api.themoviedb.org` and `developer.themoviedb.org`), so the type numbering above comes
  from TMDB's own Movie Bible page, not from a live response.
- **Recommendation**: resolve in the user's region first (reusing item 11's selected → fallback →
  any priority), preferring type **3 Theatrical**, then **4 Digital**, then falling back to
  `release_date`. **Type 1 Premiere must never be the reminder date** - telling someone their film
  is out because it screened at a festival is worse than not telling them at all.
- [ ] Put the resolved source in the notification body ("In theatres in IN on Friday"), so a date
  the user disagrees with is at least explicable. A user-facing "remind me for digital releases
  only" preference is a good *second* pass, not first-cut scope.
- TV is simple: `first_air_date`, which has no per-region equivalent.

### The CTA: where the button goes, and where it can't
- [ ] **It cannot live in `MediaActionButtonsSection`.** That composable does
  `val session = (authUiState as? AuthUiState.LoggedIn)?.session ?: return` - it renders nothing
  when signed out. A release reminder needs **no TMDB account**: it is purely local, like followed
  people and collections. Gating it behind sign-in would be a self-inflicted limitation.
- [ ] Add `ReleaseReminderButton` (`core/ui/hero/`), rendered in the movie/TV hero only when
  `isUpcoming(today)` is true. Model it on **`FollowCollectionButton`** - already a local-only,
  signed-out-capable follow toggle backed by a local table, which is precisely this shape.
- [ ] First tap calls `rememberNotificationPermissionRequester().request()` and then
  `NotificationScheduler.schedule()` - the identical pair `EpisodeAlertOptInDialog` and
  `AccountScreen`'s episode-notifications toggle already call. Do not invent a third opt-in path.
- [ ] Optional second entry point, once the detail-screen one works: the same toggle next to the
  existing `UpcomingBadge` in `AccountMediaGridContent`.

### Data model
- [ ] New **local-only** `releaseReminder` table in `MyDatabase.sq`, sitting alongside
  `favoritePerson` / `favoriteCollection` and local-only for the same documented reason - there is
  nothing on TMDB's side to sync it to. Columns: `id`, `mediaType`, `title`, `posterPath`,
  `addedAt`, `lastKnownReleaseDate TEXT` (NULL = never polled, same convention as
  `lastKnownCreditIds`), `PRIMARY KEY (id, mediaType)`.
- [ ] **Not** a column on `trackedMedia`: a reminder has to work for a title that is neither
  favorited nor watchlisted, and while signed out - cases where no `trackedMedia` row exists at all.
- [ ] **Bump `LocalSchemaVersion.CURRENT` and ship a numbered `N.sqm` migration** - mandatory for
  any `MyDatabase.sq` change (`.claude/skills/code-conventions/SKILL.md`, and `LocalSchemaVersion`'s
  own kdoc). This would be the **first real `.sqm` file since the v1.0.0 reset**, so it is also the
  moment to turn on `verifyMigrations.set(true)` in `composeApp/build.gradle.kts`'s `sqldelight`
  block, which that kdoc already flags as the thing to do alongside the first migration.
- [ ] **Add `releaseReminder` to [item 15](#15-backup--restore-for-device-only-data-the-data-tmdb-does-not-hold)'s
  backup scope** - it is device-only data with no server behind it, which is exactly that item's
  inclusion test. (Its `lastKnownReleaseDate` stays out, by the same "restore quietly" rule.)

### The poller
- [ ] `ReleaseNotificationPoller` (`features/notifications/`), same shape as the three existing
  pollers, called from `EpisodeNotificationWorker.doWork()` next to them - **one periodic job, not
  a fourth**, matching the reasoning already in `PersonCreditNotificationPoller`'s kdoc.
- [ ] Two new `NotificationReason` entries with explicit `storageValue`s (the enum's existing
  convention): `RELEASE_TOMORROW("release_tomorrow")` and `RELEASE_TODAY("release_today")`.
- [ ] `cursorValue` = the resolved release date, per the dedup property above.
- [ ] **Never fire a late "tomorrow".** If the device was off or offline through the day-before
  window, the poll must skip `RELEASE_TOMORROW` and fire only `RELEASE_TODAY` - the ledger dedups
  repeats but cannot catch a notification that is simply wrong by the time it lands. Compare the
  resolved date against `today()` and fire `RELEASE_TOMORROW` **only** when it is exactly one day
  out.
- [ ] **Age the row out.** Unlike a favorite, a reminder has a natural end: drop the row once the
  release date is ~7 days past (not immediately - a date that slips *backwards* shouldn't lose the
  row). This keeps the poll set naturally tiny.
- [ ] Per-item `try`/`catch` isolation around each detail call, catching `HttpExceptions`,
  `IOException`, `ContentConvertException` and `SerializationException` specifically - copy
  `TvEpisodeNotificationPoller.pollOne`'s existing handling, no bare `Exception`
  (`.claude/skills/code-conventions/SKILL.md`).
- [ ] Reuse `LocalNotifier.post`'s existing `posterUrl` and `deepLink` params. Tap should open the
  movie/TV detail screen, which needs a new `NotificationTarget` variant alongside
  `EpisodeNotificationTarget` in `core/notification/NotificationDeepLink.kt`, plus the
  `MovieDetailKey`/`TvDetailKey` push in `App.kt`'s `MainAppScreen`.

### Implementation Checklist
- [ ] **Schema**: `releaseReminder` table + queries (`selectAllReleaseReminders`,
  `selectReleaseReminderById`, `insertReleaseReminder`, `deleteReleaseReminder`,
  `updateLastKnownReleaseDate`, `selectReleaseRemindersForPolling`), the `.sqm` migration, and the
  `LocalSchemaVersion.CURRENT` bump.
- [ ] **Data Layer**: `ReleaseReminderRepository` / `Impl` (`features/notifications/repository/`),
  mirroring `FavoriteCollectionRepository`'s interface shape -
  `observeHasReminder(id, mediaType): Flow<Boolean>`, `setReminder(...)`,
  `remindersForPolling(): List<ReleaseReminderPollCandidate>`.
- [ ] **Date resolution**: `MovieDetail.resolveReleaseDate(regionCode): ResolvedRelease?` in
  `MovieHeroFacts.kt` next to the existing `usCertification()`/`resolveRegionCode()` helpers,
  returning the date **and** which type won so the notification body can name it. A `ReleaseType`
  **enum** (not raw ints) per the conventions' closed-value-set rule, with the TMDB integer as its
  stored value.
- [ ] **Business logic**: `ReleaseNotificationPoller` as specced above.
- [ ] **UI**: `ReleaseReminderButton` in the movie and TV heroes; all copy via `Res.string.*` in
  `composeResources/values/strings.xml` (`action_remind_me`, `action_reminder_set`,
  `notification_release_tomorrow_title/_body`, `notification_release_today_title/_body`).
- [ ] **Settings**: reminders ride the existing episode-notifications toggle's permission +
  scheduling, but get their own row on `AccountScreen` so a user can keep episode alerts and turn
  release reminders off. Add a debug "Poll release notifications now" row next to the existing
  three, with the matching `clearForReasonForDebug` call per reason.
- [ ] **Tests** (both tiers, per `.claude/skills/testing-conventions/SKILL.md`):
  - Unit (`ReleaseNotificationPollerTest`, modelled on `TvEpisodeNotificationPollerTest`): fires
    `RELEASE_TOMORROW` exactly one day out and never two days or zero days out; fires
    `RELEASE_TODAY` on the day; a missed day-before window produces only the release-day
    notification; a slipped date re-notifies while an identical re-poll does not; rows age out
    after the window; one item's HTTP failure doesn't abort the rest. Plus date resolution:
    region-theatrical beats digital beats primary, and **type 1 Premiere is never chosen**.
  - Compose UI (`ReleaseReminderButtonUiTest`): the button renders only for an upcoming title,
    renders while signed out, and tapping it toggles the reminder state.
- [ ] **Verify**: `./gradlew :composeApp:desktopTest`, `:composeApp:compileKotlinDesktop`,
  `:composeApp:assembleDebug`, `:composeApp:ktlintCheck`.

### Deliberately out of scope
- **Exact-time alarms.** Android's `SCHEDULE_EXACT_ALARM` is a restricted permission with a Play
  Console declaration attached, and day-granularity reminders do not need it. The existing 6-hour
  poll is sufficient and costs nothing new.
- **Per-title custom lead times** ("remind me a week before"). Two fixed reminders first; a lead-time
  picker is a preferences surface that should only exist if users ask for it.
- **Season-premiere reminders for shows already airing.** [Item 3a](shipped_features.md#3a-returning-series---newupcoming-episode--done-2026-08-26)
  already covers those through `next_episode_to_air` - this item is only for titles with no release
  at all yet.
- **Calendar export** (`.ics` / system calendar write). A different permission model and a different
  feature; the reminder table would be a fine source for it later.

### Risks / open questions
- **Release dates are the most volatile field TMDB has for unreleased titles.** The `cursorValue`
  design absorbs slips correctly, but the user-visible result is that dates move. A third reason
  (`RELEASE_DATE_CHANGED`) is tempting - resist it in the first cut, since three notifications per
  title is how a useful feature becomes one people turn off.
- **Desktop and JS are weak here.** Their schedulers only run while the app/tab is open, which
  matters far more for a once-ever release-day alert than for an episode poll that gets another
  chance tomorrow. The honest platform story is Android/iOS; consider not showing the CTA on
  desktop/JS rather than promising something that mostly won't fire.
- **Timezones.** A TMDB release date is a plain `LocalDate` with no timezone, and `today()` uses
  `TimeZone.currentSystemDefault()` - the same assumption `TvEpisodeNotificationPoller` already
  makes. Fine to keep, worth stating in the KDoc rather than rediscovering.
- **A reminder for a never-dated title.** TMDB frequently carries an announced film with an empty
  or year-only `release_date`. `isUpcoming()` already reads those as *not* upcoming (missing/
  unparsable dates return false), so the CTA won't appear - which is the right behaviour, but it
  means the most-anticipated titles are often exactly the ones that can't be reminded about yet.
