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

## 2. Local SQLite Database for Favorites/Watchlist (Local Notification Data Source)
**In progress on branch `feature/local-sqlite-storage`.** SQLDelight readiness research (per
target: Android/iOS/Desktop production-ready, JS/Wasm best-effort only - and why Room 3.0 isn't a
better bet right now), plus the extended schema that also covers custom lists (not just
favorites/watchlist), lives in `docs/local-storage-plan.html` (gitignored, local-only - same
pattern as items 4 and 9's linked docs).

**Goal**: Mirror the user's TMDB favorites and watchlist into an on-device SQLite database, so the
app has a fast, offline-readable local copy of "what the user is tracking" instead of hitting
`/3/account/{account_id}/favorite/*` and `/3/account/{account_id}/watchlist/*` on every screen
open. This local table is also the natural data source for
[item 3](#3-local-notifications-returning-series-favorite-actors-favorite-collections)'s
background poller - it needs to enumerate "everything the user is tracking" repeatedly on a
schedule without hammering TMDB's account endpoints on every poll tick, and it needs somewhere to
persist per-item poll state (last known `next_episode_to_air`, last seen credit ids, etc.) that a
handful of scalar `multiplatform-settings` flags doesn't model well once there's more than one of
them per tracked item.

**Not a new dependency - already half-scaffolded and unused.** `app.cash.sqldelight`
(`gradle/libs.versions.toml`'s `sqlDelight = "2.0.0"`) is already applied as a Gradle plugin
(`composeApp/build.gradle.kts:9`, root `build.gradle.kts:7`) with a driver dependency wired into
**every** platform source set already:
- `implementation(libs.sqlDelight.driver.android)` in `androidMain`
- `implementation(libs.sqlDelight.driver.sqlite)` in `desktopMain`
- `implementation(libs.sqlDelight.driver.js)` in `jsMain`
- `implementation(libs.sqlDelight.driver.native)` in `iosMain`

But the actual database definition is commented out
(`composeApp/build.gradle.kts`'s `sqldelight { databases { ... } }` block, marked `//todo`) and
`composeApp/src/commonMain/sqldelight/MyDatabase.sq` is a 0-byte placeholder file with no schema.
This reads as leftover KMP-template scaffolding from before this project's own architecture was
established - never finished or removed. Implementing this item means **finishing** that setup,
not introducing a new dependency from scratch.

### Dependency checklist (what's already there vs. what's missing):
- [x] `app.cash.sqldelight` Gradle plugin applied at root and in `composeApp`.
- [x] Platform drivers declared and already `implementation(...)`'d in every relevant source set
  (see the four bullets above) - nothing to add to `libs.versions.toml`'s dependency list itself.
- [ ] Bump `sqlDelight = "2.0.0"` in `gradle/libs.versions.toml` to current stable (`2.3.x` as of
  this research in 2026-08 - re-check the actual latest at implementation time).
- [ ] Uncomment and fill in `composeApp/build.gradle.kts`'s `sqldelight { databases { create(...)
  } }` block with a real `packageName` (e.g. `com.ajinkyabadve.kmmmywatchlist.db`) so the Gradle
  plugin actually generates the typed Kotlin API from the `.sq` file.
- [ ] Add an `expect`/`actual` `DatabaseDriverFactory` (mirrors the `WebAuthLauncher` pattern this
  codebase already uses for other per-platform primitives) so each platform constructs its
  `SqlDriver` correctly: `AndroidSqliteDriver` (needs a `Context`; Koin is already pinned in
  `libs.versions.toml` - `koin = "3.4.3"` - but currently unused anywhere in the codebase, worth
  deciding whether to finally wire it up here or just thread `Context` manually like other
  Android-only pieces do), `NativeSqliteDriver` (iOS), a JVM `sqlite-driver` pointed at a per-user
  app-data directory (desktop), and the web-worker driver plus a bundled sqlite `.wasm` binary (JS
  - confirm current SQLDelight JS/Wasm driver guidance before committing, since that target's
  story is the most likely to have shifted since the pinned `2.0.0`).

### Schema sketch (`MyDatabase.sq`):
```sql
CREATE TABLE trackedMedia (
    id INTEGER NOT NULL,
    mediaType TEXT NOT NULL,      -- "movie" | "tv" - reuses MediaTypeConstant's values
    category TEXT NOT NULL,       -- "favorite" | "watchlist"
    title TEXT NOT NULL,
    posterPath TEXT,
    addedAt INTEGER NOT NULL,           -- epoch millis, for "recently added" sorting
    lastSyncedAt INTEGER NOT NULL,
    -- Poll-state columns item 3's pollers read/write - nullable until the first poll runs.
    lastKnownNextEpisodeAirDate TEXT,   -- 3a: returning-series polling
    lastKnownCreditIds TEXT,            -- 3b: comma-separated, favorite-person polling
    PRIMARY KEY (id, mediaType, category)
);

CREATE INDEX trackedMedia_category ON trackedMedia(category);

selectByCategory:
SELECT * FROM trackedMedia WHERE category = ?;

upsert:
INSERT OR REPLACE INTO trackedMedia (id, mediaType, category, title, posterPath, addedAt, lastSyncedAt, lastKnownNextEpisodeAirDate, lastKnownCreditIds)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);

deleteMissing:
DELETE FROM trackedMedia WHERE category = ? AND id NOT IN ?;
```

### Implementation Checklist:
- [ ] **Data Layer**:
  - `TrackedMediaRepository`/`TrackedMediaRepositoryImpl` wraps the generated SQLDelight queries;
    a `sync(category)` method fetches the current favorite/watchlist pages from the already-built
    `AccountMediaRepository` ([item 6](#6-account-favorites--watchlist-replacing-my-fav-placeholder))
    and upserts them locally, then deletes local rows no longer present remotely.
  - Call `sync()` opportunistically - on app foreground and right after any favorite/watchlist
    toggle from `MediaActionButtons` - not on a fixed timer. TMDB stays the source of truth; this
    is a read-cache plus a home for local-only poll state, not an offline-write queue.
- [ ] **Business Logic**:
  - `AccountFavoritesWatchlistTab`/`AccountMediaListScreenModel` read from
    `TrackedMediaRepository` first (instant local paint) and reconcile against the network
    response once it lands, instead of today's network-only load.
  - Item 3's background poller reads its candidate set from `TrackedMediaRepository` instead of
    re-fetching every favorites/watchlist page from TMDB each poll cycle, and writes
    `lastKnownNextEpisodeAirDate`/`lastKnownCreditIds` back into the row it read from - one local
    round trip per poll instead of N TMDB calls.
- [ ] **UI Presentation**: none directly - this is a caching/data-layer change underneath the
  already-shipped Favorites/Watchlist UI ([item 6](#6-account-favorites--watchlist-replacing-my-fav-placeholder));
  no new screens.

### Deliberately out of scope here:
- No offline *write* queue (favoriting while offline, syncing later) - TMDB calls still need a
  live session regardless, and that's a materially bigger feature than what item 3's poller
  actually needs.
- No caching of full movie/TV detail payloads - just enough per-item metadata
  (title/poster/category/added date/poll state) to render a list and drive the poller; detail
  screens keep hitting TMDB directly as they do today.

---

## 3. Local Notifications (Returning Series, Favorite Actors, Favorite Collections)
**Goal**: Proactively surface changes the user would otherwise have to check for manually, via
on-device local notifications (no push infra/server needed - just periodic polling against TMDB
plus a platform notification API). All three sub-features share the same plumbing: a background
poll job, a per-item "last seen" cursor persisted locally, and a platform `expect`/`actual`
notifier - so build the shared scheduling/notification layer once, then add the three pollers on
top of it. **Depends on [#6](#6-account-favorites--watchlist-replacing-my-fav-placeholder)**: needs
real favorites/watchlist persisted first (and, for the actor case, a "favorite person" concept the
app doesn't have yet - TMDB's own account favorites only cover movies/TV, not people, so that list
would have to be app-local). **Pairs naturally with
[item 2](#2-local-sqlite-database-for-favoriteswatchlist-local-notification-data-source)**: the
poll set and per-item "last known state" this item needs is exactly what item 2's local database
is meant to hold, instead of re-fetching every favorites/watchlist page from TMDB on each poll.

### Shared infrastructure checklist:
- [x] Platform-specific background scheduler (`expect`/`actual`, mirroring the `WebAuthLauncher`
  pattern): WorkManager periodic work (Android), `BGTaskScheduler` (iOS), a JVM scheduled executor
  (Desktop), best-effort `setInterval` while the tab is open (JS). Done 2026-08-26 -
  `core/notification/NotificationScheduler.kt` + per-platform `actual`s.
- [x] Platform-specific local notification poster (`expect`/`actual`): `NotificationManager`
  (Android, channel + `POST_NOTIFICATIONS` runtime permission on API 33+),
  `UNUserNotificationCenter` (iOS, authorization request), the `Notification` Web API (JS,
  permission prompt), `SystemTray`/`TrayIcon` (Desktop). Done 2026-08-26 -
  `core/notification/LocalNotifier.kt` + `NotificationPermissionRequester.kt` + per-platform `actual`s.
  - [ ] **Manual follow-up (iOS, not yet done)**: Xcode's `INFOPLIST_KEY_` synthesis doesn't
    reliably support custom array keys, so `BGTaskSchedulerPermittedIdentifiers` (needs
    `com.ajinkyabadve.kmmmywatchlist.episodePoll`, matching `IosNotificationSchedulerConstant.TASK_IDENTIFIER`
    in `NotificationScheduler.kt`, iosMain) has to be added by hand under the `iosApp` target's Info
    tab in Xcode. `UIBackgroundModes` (fetch/processing) was added via pbxproj and doesn't need this.
    Without this, `BGTaskScheduler.sharedScheduler().submitTaskRequest(...)` will fail silently and
    the poll will only ever run via the debug-only "Poll episode notifications now" row, never in
    the background.
- [x] A "notifications" settings section: one toggle ("Episode notifications", off by default) on
  `AccountScreen` for 3a - `NotificationSettingsRepository`. A master toggle only becomes relevant
  once 3b/3c exist too.
- [x] Persist a "last notified" cursor per tracked item: `notificationLedger` SQLDelight table,
  keyed `(id, mediaType, reason)` so a poll never re-notifies for the same reason+cursor value.
- [ ] **Follow-up, not yet done (requested 2026-08-26): in-context opt-in prompt.** Right now the
  only way to discover/enable episode notifications is to already know to go dig for the
  "Episode notifications" toggle in Account settings - nothing surfaces it at the moment it'd
  actually be relevant. Instead, the first time a user favorites/watchlists a TV show (while the
  setting is still off), show a small explanatory prompt - what the notification is for ("get
  notified when this show has a new episode"), not just a bare OS permission dialog - with a
  clear opt-in action that both flips `NotificationSettingsRepository`'s toggle and requests the
  OS permission (`rememberNotificationPermissionRequester`), same as the Account row already does.
  Needs: (a) a "seen this prompt already" flag (`multiplatform-settings`, same store) so it's
  shown once, not on every favorite; (b) hooking into the favorite/watchlist toggle action -
  likely `MediaActionButtons`/wherever the heart-icon click is currently handled on detail
  screens - to trigger it only for TV media, not movies; (c) new string resources for the prompt's
  copy. Should generalize to 3b/3c once those exist (their own trigger points - favoriting a
  person/collection - rather than TV-specific).

### 3a. Returning series - new/upcoming episode — DONE (2026-08-26)
**Relevant OAS endpoints**: `GET /3/tv/{series_id}` (`status`, `next_episode_to_air.air_date`) for
watchlisted/favorited shows; `GET /3/tv/{series_id}/changes` as a cheaper diff signal.
- [x] Poll each favorited/watchlisted TV show's `next_episode_to_air`; notify once when a new
  episode's air date is newly announced, and again on the air date itself -
  `TvEpisodeNotificationPoller`.
- [x] Skip shows with `status == "Ended"` / `"Canceled"` entirely once known, so they age out of
  the poll set (zero network calls once `lastKnownStatus` records it).
- [x] **Group notifications by TV series (Android) - done 2026-08-26.** Both reasons for the same
  show (`episode_announced` and `episode_airing`, which can both fire in the same poll cycle) now
  share one `NotificationCompat` group key (`LocalNotifier.kt` androidMain,
  `AndroidNotificationConstant.GROUP_KEY_PREFIX + tvShowId`), with a summary notification per show
  so Android actually stacks them instead of showing two separate top-level entries. iOS/Desktop/JS
  don't group yet - `UNNotificationContent.threadIdentifier` is the iOS equivalent if this is
  wanted there too, not yet done.
- [x] **Episode/show poster image on the notification - done 2026-08-26.** `TvEpisodeNotificationPoller`
  resolves the episode's still image (falling back to the show's poster if no still exists) via
  `ImageConfigResolver` and passes it through `LocalNotifier.post`'s new `posterUrl` param -
  `NotificationImageFetcher` (commonMain) does the actual best-effort download, shared by the
  platforms that render it inline. Android: `NotificationCompat.BigPictureStyle` (decoded via
  `BitmapFactory`). iOS: `UNNotificationAttachment` (image written to a temp file first, since the
  API only accepts a file URL, not raw data). JS: passed straight through as the `Notification` Web
  API's `icon`/`image` options - the browser fetches it itself, no download needed on this side.
  Desktop: not supported - `java.awt.TrayIcon.displayMessage` has no image parameter at all
  (AWT/Swing tray balloons are text-only). Any failure at any step (download, decode, temp-file
  write, attachment construction) silently falls back to the existing text-only notification rather
  than losing the notification entirely - not yet covered by a dedicated test (the failure path is
  straightforward but the happy path needs a real device/simulator to see rendered).
- Verification: `TvEpisodeNotificationPollerTest` (commonTest) covers the dedup/aging/per-show
  exception-isolation cases. Real-device confirmation not yet done - see `run-app` skill's "Force-
  firing the episode-notification poll" section for the adb/debug-row steps.
- [x] **Follow-up: tap-to-episode deep link — DONE (2026-08-26)**. Tapping the notification now
  opens the TV show's detail screen and then the specific episode. `EpisodeNotificationTarget`
  (commonMain, `core/notification/NotificationDeepLink.kt`) carries `(tvShowId, seasonNumber,
  episodeNumber)` through `LocalNotifier.post`'s new `deepLink` param, populated from
  `TvEpisodeNotificationPoller`'s `detail.nextEpisodeToAir`. `PendingEpisodeNotificationTarget` is
  the observable holder `App.kt`'s `MainAppScreen` watches to push `TvDetailKey` then
  `EpisodeDetailKey`. Per-platform tap wiring: Android - `PendingIntent` extras read back in
  `AppActivity.handleNotificationIntent()` (mirrors `AndroidAuthCallbackHandler`'s intent-handling
  shape); iOS - `UNNotificationContent.userInfo`, read by `NotificationTapDelegate`
  (`UNUserNotificationCenterDelegateProtocol`, registered in `Main.kt`'s `MainViewController()`);
  JS - `Notification.onclick` (works since the app is already running - no cold-launch case);
  Desktop - `TrayIcon`'s single action listener approximates "most recently posted" (SystemTray has
  no per-message click callback). Verification: `PendingEpisodeNotificationTargetTest` (commonTest)
  covers the observable set/consume/re-tap semantics. Real-device confirmation not yet done.

### 3b. Favorite actor/person - new credit announced
**Relevant OAS endpoints**: `GET /3/person/{person_id}/combined_credits` (diff against the last
poll's credit ID set); `GET /3/person/{person_id}/changes`.
- [ ] Needs "favorite person" to exist as a concept first (see the dependency note above).
- [ ] Poll each favorited person's combined credits; notify on any new movie/TV credit id not seen
  on the previous poll, deep-linking the notification to that title's detail screen.

### 3c. New movie added to a favorited collection
**Relevant OAS endpoints**: `GET /3/collection/{collection_id}` (`parts[]`, diffed by id).
- [ ] Track collections the user has favorited a member of (e.g. favoriting a Marvel movie offers
  "follow this collection").
- [ ] Poll each followed collection's `parts`; notify when a part id appears that wasn't present on
  the previous poll (a newly-added/announced entry in the franchise).

---

## 4. Adopt Material 3 Expressive — ON HOLD, waiting on Compose Multiplatform (researched 2026-08-18)
**Goal**: Evaluate whether to adopt Google's Material 3 Expressive design language (announced
Google I/O 2025 - shape morphing, spring-based motion, bolder color-role usage, and new
components like `LoadingIndicator`/`ButtonGroup`/`SplitButton`/`FloatingToolbar`) in this app.

**Decision**: Wait for a future Compose Multiplatform release before doing any of this. Full
research, verified support matrix, and a phased plan live in
`docs/material3-expressive-plan.html` (gitignored, local-only - see item 9's follow-up note below
for the same pattern). Summary of why it's parked:

- **Verified, not assumed**: ran `./gradlew :composeApp:dependencies --configuration
  desktopCompileClasspath` to find the actual resolved artifact
  (`org.jetbrains.compose.material3:material3-desktop:1.9.0` off `compose = "1.11.1"` in
  `gradle/libs.versions.toml`), then decompiled that exact jar with `javap` rather than trusting
  release-note prose (JetBrains ports Material3 roughly one Jetpack release behind, and docs pages
  lag further).
- **What's already in the resolved jar and usable today** (not blocked, just deliberately
  deferred until the rest lands too): `MaterialExpressiveTheme`, `MotionScheme` (both
  `Standard`/`Expressive` spring-physics presets - `ExpressiveMotionSchemeImpl` and
  `StandardMotionSchemeImpl` both found), `expressiveLightColorScheme()` (light variant only, no
  dark counterpart in this jar - moot anyway since the app's palette comes from a Theme Builder
  export, not a Material default).
- **What's blocked** - only design-token classes exist in the jar (e.g. `LoadingIndicatorTokens`,
  `FloatingToolbarTokens`, `ButtonGroupSmallTokens`, `SplitButtonMediumTokens`), with **no public
  composable API shipped yet** for any of: `MaterialShapes` (shape morphing - only the existing
  `Shapes`/`DragHandleShapes` classes are present), `LoadingIndicator`, `ButtonGroup`/
  `SplitButton`, `FloatingToolbar`, the expressive FAB menu. These are exactly the components most
  associated with "Material 3 Expressive" day to day, so adopting now would mean hand-rolling what
  JetBrains is expected to ship within a release or two.
- **Codebase impact if/when this proceeds**: zero existing expressive API usage anywhere; 53 raw
  `RoundedCornerShape(...)` call sites across 27 files bypass the theme's `Shapes` object entirely
  (only 2 real `MaterialTheme.shapes.` references exist today), so a shape-morphing pass is a real
  audit project, not a config flip; 43 files read `MaterialTheme.colorScheme.`, which is broad but
  mechanical reach for any future color-role changes.
- **The one no-regrets move available right now, held off anyway for scope discipline**: flipping
  `AppTheme`'s `MaterialTheme(...)` to `MaterialExpressiveTheme(..., motionScheme =
  MotionScheme.expressive())` behind `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` is ~1
  file, fully reversible, and gives every themed transition spring-based motion for free. Revisit
  this specifically if a quick motion-feel win is wanted before the rest of Expressive lands.

**Re-check trigger**: next time a Compose Multiplatform version bump is considered, re-run the
same `javap` verification against the new `material3-desktop` jar for the blocked components
above before deciding to act.

---

## 5. Integrated Search Feature — DONE (2026-08-04)
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

## 6. Account Favorites & Watchlist (Replacing "My Fav" Placeholder) — DONE (2026-08-16)
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
    (see item 6's original card-level heart button idea - decided against it to keep the surface
    area smaller for this pass).

### Known bugs
- Poster thumbnails in `ListDetailScreen` render blank/grey instead of the actual poster image
  (movie titles/overview/rating all render correctly - just the image). Not yet root-caused; found
  during manual verification against a real account's TMDB lists on 2026-08-16.

---

## 7. Media Detailed Views (Movies & TV Shows) — DONE (2026-08-16)
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

## 8. Genre-based Discovery Screen — DONE (2026-08-18)
**Goal**: Let users filter movies and TV shows by genre, release year, or sorting.

### Relevant OAS Endpoints:
- `GET /3/genre/movie/list` & `GET /3/genre/tv/list`: Fetch available genres.
- `GET /3/discover/movie` & `GET /3/discover/tv`: Discover content using query parameters.
- `GET /3/search/keyword`: Keyword autocomplete for the `with_keywords` filter.

### Implementation Checklist:
- [x] **Data Layer**:
  - `GenreRepository`/`GenreRepositoryImpl` (`features/movies/repository/`) fetch and day-cache
    `/3/genre/movie/list` / `/3/genre/tv/list`, mirroring `ConfigurationRepositoryImpl`'s caching
    shape (genre catalogs barely change).
  - `DiscoverRepository`/`DiscoverRepositoryImpl` hit `/3/discover/movie` / `/3/discover/tv` with
    `with_genres`, `with_keywords`, `primary_release_year`/`first_air_date_year`, `sort_by`, and
    `include_adult` (threaded from the existing `RestrictedModeRepository`) - returns the existing
    `MoviePageResult`/`TvPageResult`, no new result models.
  - `KeywordRepository`/`KeywordRepositoryImpl` hits `/3/search/keyword` (not cached - query-driven
    autocomplete, unlike the genre catalog).
  - `DiscoverFilterRepository`/`DiscoverFilterRepositoryImpl` persists the *last-applied* filter set
    per media type (same `multiplatform-settings` store as region/restricted-mode/auth), defaulting
    to "last year, popularity descending, no genre/keyword restriction" until the user applies one.
- [x] **Business Logic**:
  - `DiscoverMovieScreenModel`/`DiscoverTvScreenModel` mirror `MovieListScreenModel`/
    `TvListScreenModel`'s pagination/`ListState` shape, pre-loaded from the persisted filter set in
    `init` (already loaded before the filter dialog is ever opened) and re-fetch from page one via
    `applyFilters(...)` when a new filter set is applied.
- [x] **UI Presentation**:
  - `DiscoverMovieTab`/`DiscoverTvTab` (`features/movies/screen/category/`,
    `features/tvshows/screen/category/`) - a 5th pill tab on `MovieScreenTabs`/`TvShowScreenTabs`
    (`MovieTab.Discover`/`TvTab.Discover`), alongside Now Playing/Upcoming/Popular/Top Rated and
    Airing Today/On The Air/Popular/Top Rated respectively - not a separate destination. A
    `DiscoverFilterButton` (tonal `secondaryContainer`, active-filter count badge) above the grid
    opens `DiscoverFilterDialog` (`features/discover/screen/`; year dropdown, sort-by dropdown,
    multi-select genre chips styled like `FilmographyFilterChip` but tinted with the app's
    `primaryContainer`, and a debounced keyword search field, on a `surfaceContainerHigh` dialog
    surface) - filters are staged locally and only committed on "Apply", so picking several doesn't
    trigger a reload per change.

---

## 9. TMDB User Authentication / Login — DONE (2026-08-15)
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

**Possible follow-up (not started)**: `WebAuthLauncher`'s per-platform `actual`s (Custom Tabs / iOS
`ASWebAuthenticationSession` / desktop loopback server / JS redirect) are already TMDB-agnostic
apart from three of them parsing the `request_token`/`approved` query params by name. Extracting
this into its own configurable module (and optionally a published KMP library) is feasible with
modest changes - see `docs/webauth-launcher-extraction-plan.html` (gitignored, local-only) for the
researched plan and prior-art comparison.

---

## 10. Restricted Mode Setting (Adult Content Toggle) — DONE (2026-08-17)
**Goal**: A user-facing setting - on the `AccountScreen` settings list (alongside "Log out") - to
opt in to adult content, off by default. Every TMDB list/search/discover call already takes an
`include_adult` parameter; today it's hardcoded `false` everywhere it's passed. This wires that
parameter to a real per-device setting instead.

### Relevant OAS endpoints:
No new endpoint - every existing `GET /3/search/*`, `/3/discover/*`, `/3/trending/*` call already
accepts `include_adult` (`true`/`false`).

### Implementation Checklist:
- [x] **Data Layer**:
  - `restrictedModeEnabled: Boolean` (default `true`, i.e. adult content **off**) persisted via
    `RestrictedModeRepository`/`RestrictedModeRepositoryImpl`
    (`features/settings/repository/`), the same `multiplatform-settings` store the auth session
    uses.
- [x] **Business Logic**:
  - `SearchScreenModel` threads `includeAdult = !restrictedModeRepository.isRestrictedModeEnabled()`
    into `SearchRepository`'s `include_adult` param.
  - Scope note: Discover (item 8) and Trending don't call `include_adult` anywhere yet - Discover
    isn't built, and Trending's endpoints don't expose that param the way search/discover do - so
    Search is the only wired call site today. Re-check this item if either of those change.
- [x] **UI Presentation**:
  - "Restricted Mode" `Switch` row lives in `AccountScreen`'s settings list, alongside Region/
    Fallback Region/Log out - scoped as a per-device setting, not tied to TMDB's own account-level
    adult-content flag.

---

## 11. Region Selector Driving OTT Availability — DONE (2026-08-17)
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
