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

## 3. Account Favorites & Watchlist (Replacing "My Fav" Placeholder)
**Goal**: Build a tabbed layout in the "My Fav" bottom tab where users can view their marked favorite movies/shows and their watchlist.

### Relevant OAS Endpoints:
- `GET /3/account/{account_id}/favorite/movies`: Get favorite movies.
- `GET /3/account/{account_id}/favorite/tv`: Get favorite TV shows.
- `GET /3/account/{account_id}/watchlist/movies`: Get movies watchlist.
- `GET /3/account/{account_id}/watchlist/tv`: Get TV shows watchlist.
- `POST /3/account/{account_id}/favorite`: Add/remove from favorites.
- `POST /3/account/{account_id}/watchlist`: Add/remove from watchlist.

### Implementation Checklist:
- [ ] **Data Layer**:
  - Add favorite/watchlist fetch and post functions in repositories.
- [ ] **Business Logic**:
  - Manage user authentication state or session IDs (using guest sessions `/3/authentication/guest_session/new` if needed).
  - Add a ViewModel/ScreenModel for managing favorites/watchlist.
- [ ] **UI Presentation**:
  - Build `MyFavScreenTab` with two sub-tabs: "Favorites" and "Watchlist", using our uniform centered pill tab layout.
  - Implement lists showing favorite movies and TV shows.
  - Add a "Favorite" (heart) button on media cards to mark/unmark items.

---

## 4. Media Detailed Views (Movies & TV Shows)
**Goal**: Open a comprehensive detail page when clicking on any Movie or TV Show card.

### Relevant OAS Endpoints:
- `GET /3/movie/{movie_id}` & `GET /3/tv/{series_id}`: Basic metadata.
- `GET /3/movie/{movie_id}/credits` & `GET /3/tv/{series_id}/credits`: Cast & crew.
- `GET /3/movie/{movie_id}/recommendations` & `GET /3/tv/{series_id}/recommendations`: Similar media.
- `GET /3/movie/{movie_id}/videos` & `GET /3/tv/{series_id}/videos`: Trailer video keys.

### Implementation Checklist:
- [ ] **Data Models**:
  - Create rich model classes for media details, cast members, and video keys.
- [ ] **Navigation & Routing**:
  - Add navigation routes like `movie_detail/{movie_id}` and `tv_detail/{tv_id}` to your routing configurations.
- [ ] **UI Presentation**:
  - Create a premium detailed screen Composable featuring:
    - Large backdrop banner image.
    - Title, release date, rating, runtime, and overview.
    - Horizontal scrollable cast list.
    - Trailer video player (via web link or YouTube helper).
    - Horizontal browse list of recommendations.

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


