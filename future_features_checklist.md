# Future Features Checklist (Based on TMDB OpenAPI Spec)

This document contains a checklist of high-priority features that can be implemented next, based on the endpoints defined in the TMDB OpenAPI Specification.

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
| **Supabase Edge Functions** | Supabase secrets / Vault | Supabase Auth JWT (needs a user or anon session) | Worth it only if TMDB user login (item 6) lands and its auth is wanted anyway; otherwise it's a lot of platform for one proxy. |
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

## 2. Integrated Search Feature — DONE (2026-08-04)
**Goal**: Connect the Top Bar's search bar to a functional search results screen that aggregates movies, TV shows, and people.

### Relevant OAS Endpoints:
- `GET /3/search/multi`: Search multiple content types (movies, TV, people) in a single request.

### Implementation Checklist:
- [x] **Data Layer**:
  - `SearchRepository`/`SearchRepositoryImpl` hit `/3/search/multi` with `query`, `page` and
    `include_adult=false`. `SearchResultItem` models the heterogeneous result array (movies carry
    `title`/`release_date`, TV carries `name`/`first_air_date`, people carry `profile_path` and no
    date) behind shared `displayTitle`/`imagePath`/`releaseYear` accessors.
- [x] **Business Logic**:
  - `SearchScreenModel` debounces keystrokes by 350 ms (`SEARCH_DEBOUNCE_MILLIS`) via a
    `MutableStateFlow` + `debounce` + `distinctUntilChanged` + `collectLatest` chain, so a stale
    in-flight response can't overwrite a newer query. Handles paging, retry and error states.
- [x] **UI Presentation**:
  - `SearchScreen` is pushed as its own `SearchKey` destination with the text field auto-focused via
    `FocusRequester`. Results render in one relevance-ordered adaptive grid; a `MediaTypeBadge`
    marks each card and a `scrollableChips` row (All / Movies / TV shows / People) filters
    client-side — `/3/search/multi` has no server-side type parameter, so narrowing costs no extra
    request.
  - `AppTopBar`'s `SearchBox` now navigates here instead of showing the old "Coming Soon" dialog.

### Known follow-ups:
- Filtering to a single type only narrows what's already loaded, so a type that ranks poorly for a
  query can look sparse until more pages are scrolled in.
- The `SearchScreenModel` lives in the app-wide `ViewModelStore`, so leaving and reopening search
  restores the previous query and results rather than starting blank.

---

## 3. Account Favorites & Watchlist (Replacing "My Fav" Placeholder) — DONE (2026-08-16)
**Goal**: Build a tabbed layout in the "My Fav" bottom tab where users can view their marked favorite movies/shows and their watchlist.

### Relevant OAS Endpoints:
- `GET /3/account/{account_id}/favorite/movies`: Get favorite movies.
- `GET /3/account/{account_id}/favorite/tv`: Get favorite TV shows.
- `GET /3/account/{account_id}/watchlist/movies`: Get movies watchlist.
- `GET /3/account/{account_id}/watchlist/tv`: Get TV shows watchlist.
- `POST /3/account/{account_id}/favorite`: Add/remove from favorites.
- `POST /3/account/{account_id}/watchlist`: Add/remove from watchlist.

### Implementation Checklist:
- [x] **Data Layer**:
  - `AccountMediaRepository`/`AccountMediaRepositoryImpl` (`features/account/repository/`) cover
    favorite/watchlist GET (both media types) and the shared POST toggle endpoints. GET responses
    reuse Search's `SearchPageResult`/`SearchResultItem` rather than a new model - see its kdoc.
  - Custom lists went further than originally scoped here - full CRUD via `ListsRepository`
    (`/3/list` v3 API, movie-only - see its kdoc for why, and the deferred v4/TV-list follow-up).
- [x] **Business Logic**:
  - `AccountMediaListScreenModel` (one per category × media-type pair) and `ListsScreenModel`
    mirror `MovieListScreenModel`'s pagination/`ListState` shape exactly.
  - Session comes from the existing `AuthRepository`/`UserSession` - no guest-session path needed.
- [x] **UI Presentation**:
  - `MyFavTabs` (`features/account/screen/`): three tabs - Favorites / Watchlist / Lists - via a
    `PillTabRow` extracted from `MovieScreenTabs` for reuse. Favorites/Watchlist each have a
    Movie/TV chip toggle over a paginated grid; Lists has create/view/delete plus a
    `ListDetailScreen` (add/remove items).
  - Favorite/Watchlist/Add-to-list icon buttons live on the movie/TV detail hero action row
    (`MediaActionButtons`, shared by `MovieHeroSection`/`TvHeroSection`), gated on login state -
    not on every media card as originally scoped, since that was moved to a deliberate choice
    (see item 3's original card-level heart button idea - decided against it to keep the surface
    area smaller for this pass).

### Known bugs
- Poster thumbnails in `ListDetailScreen` render blank/grey instead of the actual poster image
  (movie titles/overview/rating all render correctly - just the image). Not yet root-caused; found
  during manual verification against a real account's TMDB lists on 2026-08-16.

---

## 4. Media Detailed Views (Movies & TV Shows) — DONE (2026-08-16)
**Goal**: Open a comprehensive detail page when clicking on any Movie or TV Show card.

### Relevant OAS Endpoints:
- `GET /3/movie/{movie_id}` & `GET /3/tv/{series_id}`: Basic metadata.
- `GET /3/movie/{movie_id}/credits` & `GET /3/tv/{series_id}/credits`: Cast & crew.
- `GET /3/movie/{movie_id}/recommendations` & `GET /3/tv/{series_id}/recommendations`: Similar media.
- `GET /3/movie/{movie_id}/videos` & `GET /3/tv/{series_id}/videos`: Trailer video keys.

### Implementation Checklist:
- [x] **Data Models**:
  - `MovieDetail`/`TvDetail`/`PersonDetail`/`CollectionDetail` model classes
    (`features/{movies,tvshows,person}/model/`) cover full metadata, cast/crew, video keys.
- [x] **Navigation & Routing**:
  - `MovieDetailScreen`/`TvDetailScreen`/`PersonDetailScreen`/`CollectionDetailScreen`/
    `EpisodeDetailScreen` are pushed destinations (Voyager screen keys) from every media card
    across Trending/Movies/TV/Search/Account.
- [x] **UI Presentation**:
  - `BackdropSection`/`TvBackdropSection` render the backdrop banner with an overlaid trailer play
    button (opens the YouTube trailer via web link when a `site == "YouTube" && type == "Trailer"`
    video exists).
  - `MovieHeroSection`/`TvHeroSection` carry title, release date, rating, runtime and overview,
    themed via `HeroColors` (see `TASKS.md`'s hero-contrast work).
  - `CastSection` (horizontal scrollable cast list) and `RecommendationsSection`/
    `TvRecommendationsSection` (horizontal recommendations browse row) round out both screens.
  - Adaptive detail layout (full-screen vs. internal 50/50 split) per
    `.claude/skills/detail-screen-scroll-jank/SKILL.md` and the memory note on detail-screen split
    design.

---

## 5. Genre-based Discovery Screen
**Goal**: Let users filter movies and TV shows by genre, release year, language, or popularity sorting.

### Relevant OAS Endpoints:
- `GET /3/genre/movie/list` & `GET /3/genre/tv/list`: Fetch available genres.
- `GET /3/discover/movie` & `GET /3/discover/tv`: Discover content using query parameters.

### Implementation Checklist:
- [ ] **Data Layer**:
  - Add endpoints to fetch genres and fetch discovered items using dynamic query filters.
- [ ] **UI Presentation**:
  - Create a "Discover" search filter panel (e.g. bottom sheet or side panel on desktop) letting users pick genres (Action, Drama, Comedy) and filter parameters.
  - Render lists using our common scrollable grid screen content.

---

## 6. TMDB User Authentication / Login — DONE (2026-08-15)
**Goal**: Allow users to log in securely using their TMDB credentials to sync favorites, watchlist, and ratings.

### Relevant OAS Endpoints:
- `GET /3/authentication/token/new`: Create a request token.
- `POST /3/authentication/session/new`: Create a session ID with an authorized request token.
- `DELETE /3/authentication/session`: Delete a session (Log out).

### Implementation Checklist:
- [x] **Data Layer**:
  - `AuthRepository`/`AuthRepositoryImpl` creates request tokens (`/3/authentication/token/new`), exchanges authorized tokens for session IDs (`/3/authentication/session/new`), fetches account details (`/3/account`), and manages session expiration events.
  - Local session state (`UserSession`) is persisted securely using `multiplatform-settings` across platforms.
- [x] **UI Presentation & Flow**:
  - `MyFavScreenTab` and `MyFavScreenModel` handle authenticated vs unauthenticated UI states seamlessly (showing user account details, avatar, and logout option when signed in, or login prompt when signed out).
  - Platform-specific `WebAuthLauncher` handles opening browser auth URLs (`https://www.themoviedb.org/authenticate/{request_token}?redirect_to=mywatchlist://auth-callback`) and catching callbacks for Android, iOS, Desktop, and JS targets.

---

## 7. Local Notifications (Returning Series, Favorite Actors, Favorite Collections)
**Goal**: Proactively surface changes the user would otherwise have to check for manually, via
on-device local notifications (no push infra/server needed - just periodic polling against TMDB
plus a platform notification API). All three sub-features share the same plumbing: a background
poll job, a per-item "last seen" cursor persisted locally, and a platform `expect`/`actual`
notifier - so build the shared scheduling/notification layer once, then add the three pollers on
top of it. **Depends on [#3](#3-account-favorites--watchlist-replacing-my-fav-placeholder)**: needs
real favorites/watchlist persisted first (and, for the actor case, a "favorite person" concept the
app doesn't have yet - TMDB's own account favorites only cover movies/TV, not people, so that list
would have to be app-local).

### Shared infrastructure checklist:
- [ ] Platform-specific background scheduler (`expect`/`actual`, mirroring the `WebAuthLauncher`
  pattern): WorkManager periodic work (Android), `BGTaskScheduler` (iOS), a JVM scheduled executor
  (Desktop), skip or best-effort `setInterval` while the tab is open (JS - no background execution
  there).
- [ ] Platform-specific local notification poster (`expect`/`actual`): `NotificationManager`
  (Android, needs a channel + `POST_NOTIFICATIONS` runtime permission on API 33+),
  `UNUserNotificationCenter` (iOS, needs authorization request), the `Notification` Web API (JS,
  needs permission prompt), a tray notification or no-op (Desktop).
- [ ] A "notifications" settings section (ties into item 8 for the permission/settings UI shape)
  with a master toggle plus one toggle per sub-feature below.
- [ ] Persist a "last notified" cursor per tracked item (`multiplatform-settings`, same store the
  auth session already uses) so a poll never re-notifies for something already surfaced.

### 7a. Returning series - new/upcoming episode
**Relevant OAS endpoints**: `GET /3/tv/{series_id}` (`status`, `next_episode_to_air.air_date`) for
watchlisted/favorited shows; `GET /3/tv/{series_id}/changes` as a cheaper diff signal.
- [ ] Poll each favorited/watchlisted TV show's `next_episode_to_air`; notify once when a new
  episode's air date is newly announced, and again on the air date itself.
- [ ] Skip shows with `status == "Ended"` / `"Canceled"` entirely once known, so they age out of
  the poll set.

### 7b. Favorite actor/person - new credit announced
**Relevant OAS endpoints**: `GET /3/person/{person_id}/combined_credits` (diff against the last
poll's credit ID set); `GET /3/person/{person_id}/changes`.
- [ ] Needs "favorite person" to exist as a concept first (see the dependency note above).
- [ ] Poll each favorited person's combined credits; notify on any new movie/TV credit id not seen
  on the previous poll, deep-linking the notification to that title's detail screen.

### 7c. New movie added to a favorited collection
**Relevant OAS endpoints**: `GET /3/collection/{collection_id}` (`parts[]`, diffed by id).
- [ ] Track collections the user has favorited a member of (e.g. favoriting a Marvel movie offers
  "follow this collection").
- [ ] Poll each followed collection's `parts`; notify when a part id appears that wasn't present on
  the previous poll (a newly-added/announced entry in the franchise).

---

## 8. Restricted Mode Setting (Adult Content Toggle)
**Goal**: A user-facing setting - on the `AccountScreen` settings list (alongside "Log out") - to
opt in to adult content, off by default. Every TMDB list/search/discover call already takes an
`include_adult` parameter; today it's hardcoded `false` everywhere it's passed. This wires that
parameter to a real per-device setting instead.

### Relevant OAS endpoints:
No new endpoint - every existing `GET /3/search/*`, `/3/discover/*`, `/3/trending/*` call already
accepts `include_adult` (`true`/`false`).

### Implementation Checklist:
- [ ] **Data Layer**:
  - Add `restrictedModeEnabled: Boolean` (default `true`, i.e. adult content **off**) to the same
    `multiplatform-settings` store the auth session uses, behind a small `SettingsRepository` (this
    app has no general app-settings repository yet - auth is the only thing persisted today).
- [ ] **Business Logic**:
  - Thread `includeAdult = !restrictedModeEnabled` through every repository call site that
    currently hardcodes `include_adult=false` (search, discover, trending) instead of the literal.
- [ ] **UI Presentation**:
  - Add a "Restricted Mode" toggle row (`Switch`, not a navigating chevron row) to `AccountScreen`'s
    settings list - likely gated so it only shows once TMDB's own account-level adult-content
    setting is considered, or clearly scoped as "this device" if it stays local-only.
  - Consider whether toggling it off mid-session should also filter results already cached in
    memory, or only apply going forward.

---

## 9. Region Selector Driving OTT Availability — DONE (2026-08-17)
**Goal**: Let the user pick their region instead of the app silently falling back to
[`RegionConstant.US`](../composeApp/src/commonMain/kotlin/com/ajinkyabadve/kmmmywatchlist/core/constant/RegionConstant.kt)
whenever the device locale has no TMDB entry for watch providers. Watch-provider display ("Watch
on Amazon Prime Video" etc., see `MovieHeroFacts`/`TvHeroSection`) already existed but was not
user-adjustable.

### Relevant OAS endpoints:
- `GET /3/watch/providers/regions`: Regions TMDB actually has watch-provider data for - used
  instead of the full `/3/configuration/countries` list so the picker never offers an empty region.
- `GET /3/movie/{movie_id}/watch/providers` & `GET /3/tv/{series_id}/watch/providers`: Already
  called - keyed by region in the response (`results.{region_code}`).

### Implementation Checklist:
- [x] **Data Layer**:
  - `RegionRepository`/`RegionRepositoryImpl` (`features/settings/repository/`) fetch and
    week-long-cache `/3/watch/providers/regions` (mirrors `ConfigurationRepositoryImpl`'s caching
    shape), and get/set two persisted `multiplatform-settings` values: the selected region
    (defaults to device locale) and a separate fallback region (defaults to `RegionConstant.US`,
    also user-configurable - see below).
- [x] **Business Logic**:
  - `WatchProvidersResponse?.resolveRegion(regionCode, fallbackRegionCode)`
    (`MovieHeroFacts.kt`) replaced the old hardcoded-US fallback chain; `MovieHeroSection`,
    `TvHeroSection`, and `MovieMetaSection`'s "Where to watch" all resolve through the
    persisted selected/fallback region now, sourced once per screen load in
    `MovieDetailScreenModel`/`TvDetailScreenModel`.
  - Deliberate scope cut: content ratings/release-dates lookups (`usCertification()`) still read
    `RegionConstant.US` directly - only watch-provider region was in scope. Also, changing the
    region does not retroactively refresh an already-open detail screen (resolved once at load) -
    confirmed acceptable by the user rather than implemented.
- [x] **UI Presentation**:
  - `AccountScreen`'s settings list has "Region" and "Default fallback region" rows, each opening
    `RegionPickerDialog` (search + list, `features/auth/screen/RegionPickerDialog.kt`) backed by
    `RegionScreenModel`. Each region row shows a flag emoji
    (`core/format/RegionFlag.kt`'s `toRegionFlagEmoji()`, built from Unicode Regional Indicator
    Symbols - no bundled flag images).


