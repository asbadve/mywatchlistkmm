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

## 2. Integrated Search Feature
**Goal**: Connect the Top Bar's search bar to a functional search results screen that aggregates movies, TV shows, and people.

### Relevant OAS Endpoints:
- `GET /3/search/multi`: Search multiple content types (movies, TV, people) in a single request.

### Implementation Checklist:
- [ ] **Data Layer**:
  - Add search methods to repositories (or create a dedicated `SearchRepository`) to hit `/3/search/multi` passing a query string.
- [ ] **Business Logic**:
  - Create a `SearchScreenModel` to handle search input flow, debouncing keystrokes, and loading states.
- [ ] **UI Presentation**:
  - Design a `SearchScreenContent` displaying categorized search results (e.g., grouped by Movies, TV Shows, and People).
  - Update `AppTopBar` in `App.kt` to wire text input from the search field to launch the search view.

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

## 6. TMDB User Authentication / Login
**Goal**: Allow users to log in securely using their TMDB credentials to sync favorites, watchlist, and ratings.

### Relevant OAS Endpoints:
- `GET /3/authentication/token/new`: Create a request token.
- `POST /3/authentication/session/new`: Create a session ID with an authorized request token.
- `DELETE /3/authentication/session`: Delete a session (Log out).

### Implementation Checklist:
- [ ] **Data Layer**:
  - Add authentication and session generation requests to the networking module.
  - Implement secure storage for session keys locally (e.g., using settings/keychain library).
- [ ] **UI Presentation & Flow**:
  - Create a "Settings" or "Profile" screen layout.
  - Integrate a login mechanism (WebView overlay or redirect to TMDB auth URL `https://www.themoviedb.org/authenticate/{request_token}`) for authorizing request tokens.
  - Implement dynamic UI states based on whether the user is logged in or logged out.

