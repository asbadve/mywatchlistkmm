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

## 2. Local SQLite Database for Favorites/Watchlist (Local Notification Data Source) — DONE
**Built on branch `feature/local-sqlite-storage`**, and gone well beyond the original scope below -
it now backs Favorites/Watchlist, custom lists, notification poll state, and a
`NetworkBoundResource`-style detail cache, not just a favorites/watchlist mirror. SQLDelight
readiness research that kicked this off (per target: Android/iOS/Desktop production-ready,
JS/Wasm best-effort only - and why Room 3.0 wasn't a better bet) lives in
`docs/local-storage-plan.html` (gitignored, local-only - same pattern as items 4 and 9's linked
docs).

**What actually shipped, vs. the original sketch below**: the plan called for a simple
`sync(category)` method that fetches TMDB pages and upserts/deletes them locally. What was built
instead is a full **Paging3 `RemoteMediator`** (`TrackedMediaRemoteMediator`) - the local
`trackedMedia` table *is* the paging source (`QueryPagingSource` over `selectByCategoryPaged`),
fed page-by-page from TMDB only as the grid scrolls, rather than a bulk sync pass. Toggling
favorite/watchlist off marks a row `pendingDelete` immediately (optimistic local hide, works
offline) and only hard-deletes it once TMDB confirms - see `TrackedMediaRepository.markPendingDelete`/
`clearPendingDelete`/`confirmDelete`, wired from `MediaActionsState`. `MyDatabase.sq` grew well past
the `trackedMedia` sketch: `customList`/`customListItem` (item 6's Lists feature, with the same
pending-delete pattern), `movieDetailCache`/`tvDetailCache`/`tvSeasonDetailCache`/
`personDetailCache` (full detail-screen payloads, see "Deliberately out of scope" below - this was
done anyway), `remoteKeys` (Paging3's own bookkeeping), `notificationLedger` and `favoritePerson`
(item 3's dedup/local-follow state).

### Dependency checklist:
- [x] `app.cash.sqldelight` Gradle plugin applied at root and in `composeApp`.
- [x] Platform drivers declared in every relevant source set (`android`/`sqlite`/`js`/`native`).
- [x] `sqlDelight` bumped from `2.0.0` to `2.3.2` in `gradle/libs.versions.toml`.
- [x] `composeApp/build.gradle.kts`'s `sqldelight { databases { create("MyDatabase") { ... } } }`
  block filled in: `packageName.set("com.ajinkyabadve.kmmmywatchlist.db")`,
  `generateAsync.set(true)` (required for the JS target's async `WebWorkerDriver` - every
  platform's generated queries API is suspend-based as a result), and
  `dialect(libs.sqlDelight.dialect.sqlite338)` (the default dialect predates the
  `ON CONFLICT ... DO UPDATE` syntax the upsert queries use).
- [x] `expect`/`actual` `DatabaseDriverFactory` on all four platforms (`androidMain`/`desktopMain`/
  `iosMain`/`jsMain`), behind an `AppDatabaseProvider` singleton accessor.

### What's different from the schema sketch:
- [x] `trackedMedia` table built, but keyed/shaped for paging + optimistic delete rather than a
  flat sync mirror - see `MyDatabase.sq`'s `trackedMedia`/`selectByCategoryPaged`/
  `markTrackedMediaPendingDelete` and the RemoteMediator note above.
- [x] Poll-state columns (`lastKnownNextEpisodeAirDate`, `lastKnownCreditIds`, plus
  `lastKnownStatus` which the sketch didn't anticipate, for skipping ended/canceled shows) - all
  present and read/written by item 3's pollers via `TrackedMediaRepository`.
- **The JS/web target does not use this table at all.** `NetworkOnlyTrackedMediaRepositoryImpl`
  (jsMain) skips local storage entirely and hits TMDB directly - a deliberate scope decision (see
  its kdoc), not a gap: web has no offline story to begin with, so the sql.js/`WebWorkerDriver`
  bundling cost wasn't worth it for that target. `trackedTvForPolling()` returns empty there.

### Implementation Checklist:
- [x] **Data Layer**: `TrackedMediaRepository`/`SqliteTrackedMediaRepositoryImpl` wrap the
  generated SQLDelight queries; `TrackedMediaRemoteMediator` (not a `sync()` method - see above)
  keeps the local table in step with `AccountMediaRepository` ([item 6](#6-account-favorites--watchlist-replacing-my-fav-placeholder))
  as the grid scrolls. `TrackedMediaRepositoryImplTest`/`TrackedMediaRemoteMediatorTest`
  (desktopTest) and `FakeTrackedMediaRepository` (commonTest) cover it.
- [x] **Business Logic**: `AccountMediaListScreenModel` reads from
  `TrackedMediaRepository.pagedFlow` (local-first, `cachedIn(viewModelScope)`), not a network-only
  load. Item 3's `TvEpisodeNotificationPoller`/`PersonCreditNotificationPoller` read their
  candidate sets from `TrackedMediaRepository.trackedTvForPolling()`/`favoritePerson`, and write
  poll state straight back into the row they read from.
- [x] **UI Presentation**: none directly, as planned - it's a caching/data-layer change underneath
  the already-shipped Favorites/Watchlist UI.

### Scope that grew beyond the original plan:
- **Full movie/TV/season/person detail-payload caching was explicitly out of scope below - built
  anyway.** `MovieDetailCacheRepository`/`TvDetailCacheRepository` back `MovieDetailScreenModel`
  via the new `NetworkBoundResource` (`core/data/NetworkBoundResource.kt`, a Kotlin/Flow port of
  Google's "Guide to app architecture" sample): peek the local cache, decide whether to fetch,
  fetch-and-save on success, fall back to the cache on failure/offline. Every detail screen repeats
  this shape by hand today; `NetworkBoundResource` does the orchestration once.
- **Custom lists (item 6) got the same local-cache treatment** (`customList`/`customListItem`
  tables, same pending-delete pattern as `trackedMedia`) - not called out at all in the original
  plan, which only scoped favorites/watchlist.
- No offline *write* queue for favoriting/listing itself still holds as a boundary - TMDB calls
  still need a live session; only already-in-flight toggles get the optimistic local hide.

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
  - [x] **Manual follow-up (iOS) - done 2026-08-27.** Xcode's `INFOPLIST_KEY_` synthesis doesn't
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
- [x] **In-context opt-in prompt — DONE (2026-08-26).** Designed first as a Claude artifact
  ("Episode Alerts Prompt", built from the app's real `theme/Color.kt` M3 tokens) before
  implementation, per the same design-first pass the app icon got. The first time a TV show is
  favorited or watchlisted while `NotificationSettingsRepository.isEpisodeNotificationsEnabled()`
  is still false, `MediaActionButtonsSection` shows `EpisodeAlertOptInDialog` (an `AlertDialog`,
  matching `AddToListDialog`'s existing pattern rather than introducing `ModalBottomSheet`) with
  the show's name substituted into the copy. `MediaActionsState.shouldPromptForEpisodeAlerts`
  (a `StateFlow<Boolean>`) is the trigger - set on `toggleFavorite`/`toggleWatchlist` only when
  the new value is `true` and `mediaType == MediaTypeConstant.TV`, so movies and turning
  favorite/watchlist *off* never fire it. `NotificationSettingsRepository.hasSeenEpisodeAlertOptInPrompt`/
  `markEpisodeAlertOptInPromptSeen` (new `multiplatform-settings` key) gate it to once ever,
  regardless of which action (confirm or "Not now") the user takes. Confirming calls
  `rememberNotificationPermissionRequester().request()` then `setEpisodeNotificationsEnabled(true)`
  + `NotificationScheduler.schedule()` - the identical pair the Account row's toggle already
  calls, just reached from a second entry point. `MediaActionButtonsSection` gates the whole
  feature on a new `tvShowName: String?` parameter (only ever passed non-null from
  `TvDetailScreen`, via `TvDetail.title`) so movies structurally can't show it even if the state
  flow somehow flipped. Unit tests in `MediaActionsStateTest`/`NotificationSettingsRepositoryImplTest`,
  Compose UI tests in `MediaActionButtonsUiTest` (prompt shows on TV favorite/watchlist, never on
  movie, both actions mark it seen and close it). Generalizing to 3b/3c can reuse the same
  `shouldPromptForEpisodeAlerts` shape once those features have their own trigger points.

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
  `TvEpisodeNotificationPoller`'s `detail.nextEpisodeToAir`. `PendingNotificationTarget` is
  the observable holder `App.kt`'s `MainAppScreen` watches to push `TvDetailKey` then
  `EpisodeDetailKey`. Per-platform tap wiring: Android - `PendingIntent` extras read back in
  `AppActivity.handleNotificationIntent()` (mirrors `AndroidAuthCallbackHandler`'s intent-handling
  shape); iOS - `UNNotificationContent.userInfo`, read by `NotificationTapDelegate`
  (`UNUserNotificationCenterDelegateProtocol`, registered in `Main.kt`'s `MainViewController()`);
  JS - `Notification.onclick` (works since the app is already running - no cold-launch case);
  Desktop - `TrayIcon`'s single action listener approximates "most recently posted" (SystemTray has
  no per-message click callback). Verification: `PendingNotificationTargetTest` (commonTest)
  covers the observable set/consume/re-tap semantics.
  - [ ] **Known bug (Desktop only, confirmed 2026-08-27): clicking the notification banner itself
    does not deep-link.** `java.awt.TrayIcon`'s `ActionListener` reliably fires when the *tray icon*
    in the menu bar is clicked, but does not reliably fire when the transient notification banner
    is clicked - this is a documented cross-platform AWT limitation, not a bug in this app's code
    (see [JDK-7029240](https://bugs.java.com/bugdatabase/view_bug?bug_id=7029240) and
    [JDK-8146537](https://bugs.openjdk.org/browse/JDK-8146537)). Plain AWT has no per-notification
    click callback at all. Real-device confirmation of Android/iOS/JS tap-to-episode is still not
    done either.
  - [ ] **Future: replace `java.awt.TrayIcon` with [ComposeNativeTray](https://github.com/kdroidFilter/ComposeNativeTray)**
    to fix the bug above - it wraps native tray/notification APIs per OS and exposes a primary-
    action callback that macOS/Windows actually fire on a notification-banner click, unlike AWT.
    Also would unlock desktop poster images (`posterUrl` currently unsupported on this platform,
    see the poster-image item above) if the library's notification API accepts one. Not started -
    requires adding the dependency and rewriting `desktopMain`'s `LocalNotifier.kt`.

### 3b. Favorite actor/person - new credit announced — DONE (2026-08-26)
**Relevant OAS endpoints**: `GET /3/person/{person_id}/combined_credits` (diff against the last
poll's credit ID set); `GET /3/person/{person_id}/changes`.

**TMDB API check, ground-truthed against the live OpenAPI docs before building anything**: TMDB has
**no account-level favorite/follow API for people** - `POST /3/account/{account_id}/favorite`'s own
spec says "mark a movie or TV show as a favourite," and the endpoint index lists only Favorite
Movies/Favorite TV (same for ratings: Rated Movies/TV/TV Episodes, no Rated People). So favoriting
a person is **local-only, does not sync across devices** - a new `favoritePerson` SQLDelight table
(`MyDatabase.sq`), not a `trackedMedia` row (people aren't media, nothing to sync from a GET).
- [x] "Favorite person" concept: `FavoritePersonRepository` (local SQLite, `observeIsFavorite`
  Flow-backed) + `FollowPersonButton` on `PersonHeroSection` (pure composable, no repository -
  `PersonDetailScreenModel` owns the repository, per code-conventions §6/§7/§8).
- [x] `PersonCreditNotificationPoller` polls each favorited person's `combined_credits`
  (`PersonRepository.getPersonDetails` already appends it) and notifies on any credit id not seen
  on the previous poll, via the same `NotificationLedgerRepository`/`NotificationScheduler`/
  `LocalNotifier` infrastructure 3a built (`NotificationReason.PERSON_NEW_CREDIT`) - one shared
  periodic job/permission/setting covers both 3a and 3b, not a second toggle.
- [x] In-context notification opt-in prompt for following a person too (requested 2026-08-26,
  mirrors 3a's own follow-up): `NotificationOptInDialog` (renamed/generalized from
  `EpisodeAlertOptInDialog`, now a shared shell taking plain `title`/`body` strings) shows on the
  first person followed while notifications are off, same `NotificationSettingsRepository`
  seen-flag/permission-request/`NotificationScheduler.schedule()` sequence as the TV prompt.
  `AccountScreen`'s "Poll episode notifications now" debug row already resets/polls both 3a and 3b
  together (shared `notificationLedger`), and the debug "reset opt-in prompt" row now covers both
  the TV and person prompts too (one shared seen-flag).
  **Revised 2026-08-27** after it was observed firing on screen *load* instead of on the Follow
  click: the trigger was originally a ViewModel-owned `StateFlow`
  (`PersonDetailScreenModel.shouldPromptForNotificationOptIn`, mirroring
  `MediaActionsState.shouldPromptForEpisodeAlerts`), but `viewModel(key = "PersonDetailScreenModel:
  $personId")` can outlive a single screen visit - this app's `NavDisplay` has no per-entry
  `ViewModelStore` scoping, unlike a plain class such as `MediaActionsState` that's rebuilt fresh
  by its owning ScreenModel each time. A flag living on that ViewModel risked surfacing on a later,
  click-free revisit. Fixed by making `toggleFollowPerson` return whether the call just turned
  following ON, and deciding whether to show the dialog directly at the click site in
  `PersonDetailScreen` (a local `remember { mutableStateOf(false) }`), never via a persisted flag -
  see `toggleFollowPerson`'s kdoc. The permission-requester/coroutine-scope hoisting fix TV's prompt
  needed (see `MediaActionButtonsSection`'s kdoc) still applies here at `PersonDetailScreen`'s top
  level, same reasoning.
- [x] Local-only-storage caveat (requested 2026-08-27): since following a person is never synced to
  TMDB (unlike movie/TV favorites), the user needs to be told so explicitly, not just left to
  discover it - `FollowPersonButton` shows a small caption once followed
  ("Saved on this device only..."), and the new Favorites sub-tab below shows the same caveat
  unconditionally. The Follow button itself was never gated behind a signed-in session to begin
  with (nothing here needs `AccountId`/`sessionId`), so it already showed the same regardless of
  login state - this only adds the explanation, not a behavior change.
- [x] "Favorites" sub-tab on the Person destination (requested 2026-08-26), alongside the existing
  "Popular" sub-tab: `PersonScreenTab` now renders a `PillTabRow` (the same tab chrome
  `MovieScreenTabs`/`MyFavTabs` use) with both. `FavoritePersonRepository.observeFavoritePeople()`
  (new, reactive, most-recently-followed first) feeds `PersonFavoritesTab`, which reuses the exact
  same `mediaPersonRow` grid item the Popular sub-tab already renders with - deliberately not a new
  icon/card style, so both grids are visually identical (the consistency this was explicitly asked
  for). Empty state uses the same heart-outline icon language as `FollowPersonButton`.
  **Also surfaced (2026-08-27) while building this**: JetBrains' Compose Multiplatform string
  resource compiler does not unescape `\'` the way Android's `aapt` does - it renders the literal
  backslash. Every `strings.xml` apostrophe added across this session's work used that escape and
  was silently wrong until a Compose UI test asserted the exact string; fixed by writing apostrophes
  bare (valid XML element content, no escaping needed there at all) and documented in
  code-conventions §2 so it isn't reintroduced.
- [x] **Fixed 2026-08-27: first poll after following someone notified once per existing credit.**
  `lastKnownCreditIds` starts `null` (never polled); the diff was against an empty set on that
  first poll, so every credit a person already had - their whole filmography, for anyone prolific -
  counted as "new" and notified. `PersonCreditNotificationPoller.pollOne` now treats a `null`
  baseline as seed-only: it records the current credit set and returns without notifying for any
  of it. Only credits that appear on a *later* poll, after that baseline exists, ever notify - the
  intended "let me know when they book something new" behavior, not "list everything they've ever
  done." `PersonCreditNotificationPollerTest` covers this explicitly
  (`testFirstPollAfterFollowingSeedsBaselineWithoutNotifying`).
- [x] **Split the debug testing rows 2026-08-27** (requested, so TV and person notification
  testing don't interfere): `AccountScreen`'s single combined "Poll episode notifications now" row
  is back to TV-only, unchanged from before 3b touched it. A new, separate "Poll person
  notifications now" row does the 3b equivalent. Needed a real fix underneath, not just a UI split:
  `NotificationLedgerRepository.clearAllForDebug()` (whole-table wipe) became
  `clearForReasonForDebug(reason)` so each row only ever clears its own reason's dedup rows, never
  the other's. And `FavoritePersonRepository.resetAllCreditStateForDebug()` had to stop resetting
  to `NULL` - under the first-poll-seeds-only fix above, a `NULL` reset would make the debug row
  silently re-baseline instead of forcing a notification, defeating its purpose - it resets to `""`
  instead (a real but empty baseline, so every current credit counts as new on the next poll). See
  `MyDatabase.sq`'s `favoritePerson` table kdoc for the `NULL`-vs-`""` distinction this now depends on.
- [x] **Group notifications by person id, Android, 2026-08-27** (requested - same behavior 3a's
  episode notifications already had per show, just missing for 3b): `LocalNotifier`'s Android
  actual now computes a group key for `PersonNotificationTarget` too
  (`PERSON_GROUP_KEY_PREFIX + personId`, a distinct prefix from TV's so a person id and a tvShowId
  that happen to be numerically equal can never merge into one stack), with its own group-summary
  title ("New credits" vs. TV's "Episode updates"). Several new credits for the same favorited
  person now collapse into one expandable stack instead of flooding the notification shade one row
  each. iOS has no grouping mechanism at all yet for *either* notification kind (no
  `UNNotificationContent.threadIdentifier` set) - a pre-existing gap, not something this pass
  introduced or was asked to close.
- [x] **Fixed 2026-08-27: the person debug-test row itself was the "too many notifications" cause
  the grouping work above didn't fully explain.** Real-world traffic was already minimal (0 or 1
  new credit per person per ~6h poll, thanks to the first-poll-seeds-only fix above) - but
  `resetAllCreditStateForDebug()` reset every favorited person's baseline to `""` (empty, not
  `null`), which makes *every current credit* look new at once on the next poll - by design, back
  when a person's dedup state only had two values worth distinguishing. For someone with a real
  filmography that's still dozens of notifications per tap. Replaced with
  `PersonCreditNotificationPoller.seedOneNewCreditForDebug()`: it fetches each favorited person's
  actual current credits and holds back exactly one, so the debug row's forced poll notifies once
  per person - never once per credit - matching the one-notification-per-tap shape
  `resetAllTvPollStateForDebug` already gives TV (which never had this problem since a show only
  ever has one next episode at a time). `resetAllCreditStateForDebug()` and its SQL query
  (`clearAllFavoritePersonCreditIdsForDebug`) were removed as dead code, not just deprecated.
- [x] Tap-to-navigate deep link, Android + iOS (confirmed 2026-08-26 not to include Desktop/JS):
  `NotificationTarget` is now a `sealed interface` (`EpisodeNotificationTarget`/
  `PersonNotificationTarget`, `NotificationDeepLink.kt`) - a 3b notification carries
  `PersonNotificationTarget(personId)` and a tap opens that person's detail screen
  (`PersonDetailKey`, `App.kt`'s `MainAppScreen`). Desktop's `TrayIcon` and JS's
  `Notification.onclick` actuals only ever act on `EpisodeNotificationTarget` - a deliberate scope
  decision (Desktop already had this limitation for 3a; JS matched it to stay consistent), not a
  capability gap - both platforms still show the notification either way.
- [ ] **Deferred, separate pass: optional cross-device sync.** TMDB can't carry this, so syncing a
  favorited-person list across a user's devices needs this app's own small backend keyed by their
  TMDB `account_id`. Evaluated cheap/open-source options (2026-08-26): **Supabase** (open-source,
  Postgres + plain-HTTP REST reachable from every KMM target with no native SDK; free tier is 500MB
  DB/50k MAU, but auto-pauses a project after 7 days with no traffic) vs. **PocketBase**
  (single-binary, fully open-source, self-hosted - $0 indefinitely on Fly.io's free 1GB-volume tier
  or Oracle Cloud's Always-Free tier, no auto-pause, but you own uptime/backups/TLS yourself;
  Render's free tier does *not* work for it, no persistent storage). Recommendation if/when built:
  PocketBase on Fly.io - the sync payload (a handful of person ids per user) doesn't need Supabase's
  managed-Postgres muscle, and $0-with-no-pause suits a personal project better than a managed DB
  that needs to stay warm.

### 3c. New movie added to a favorited collection — DONE
**Relevant OAS endpoints**: `GET /3/collection/{collection_id}` (`parts[]`, diffed by id).

**TMDB API check, ground-truthed against the live OpenAPI docs before building anything**: same
finding as 3b's favorite-person check - TMDB has **no account-level favorite/follow API for
collections either**. `POST /3/account/{account_id}/favorite`'s `media_type` only accepts
`"movie"`/`"tv"`, and the collection reference pages (`Collection Details`, `Collection Images`)
are read-only. So following a collection is **local-only, does not sync across devices** - a new
`favoriteCollection` SQLDelight table (`MyDatabase.sq`), same shape as `favoritePerson`.

- [x] "Favorite collection" concept: `FavoriteCollectionRepository` (local SQLite,
  `observeIsFavorite` Flow-backed) + `FollowCollectionButton` on `CollectionDetailScreen`'s header
  (pure composable, no repository - `CollectionDetailScreenModel` owns the repository, per
  code-conventions §6/§7/§8), with the same "saved on this device only" caveat
  `FollowPersonButton` shows.
- [x] `CollectionNotificationPoller` polls each favorited collection's `parts` (via the already-
  built `MovieRepository.getCollectionDetails`) and notifies on any part id not seen on the
  previous poll, via the same `NotificationLedgerRepository`/`NotificationScheduler`/
  `LocalNotifier` infrastructure 3a/3b built (`NotificationReason.COLLECTION_NEW_PART`) - one
  shared periodic job/permission/setting covers all three, not a third toggle. Carries the same
  first-poll-seeds-baseline-without-notifying fix 3b needed (a 20-film franchise doesn't fire 20
  notifications the moment it's followed) from day one, rather than as a later bug fix.
- [x] In-context notification opt-in prompt on `CollectionDetailScreen`, same
  `NotificationOptInDialog` shell 3a/3b already share.
- [x] Tap-to-navigate deep link, Android + iOS only (matches 3b's `PersonNotificationTarget` scope
  decision - Desktop/JS still show the notification, just don't act on a tap):
  `CollectionNotificationTarget(collectionId)` opens `CollectionDetailKey`.
- [x] **"Collections" tab under My Fav** (requested alongside this item, not left for a later
  ask like 3b's Person sub-tab was): `MyFavTabs` grew a 4th `PillTabRow` entry,
  `FavoriteCollectionsTab` (`features/account/screen/`) lists every locally-followed collection via
  `FavoriteCollectionRepository.observeFavoriteCollections()`, reusing the same `mediaPersonRow`
  grid item `PersonFavoritesTab` renders with (not actually person-specific despite its package).
  Same local-only caveat and empty-state treatment as the Person tab's equivalent.
- Debug: `AccountScreen`'s "Poll collection notifications now" row mirrors the person one
  (`seedOneNewPartForDebug` holds back one part so a forced poll notifies once per collection, not
  once per part - same reasoning as `resetAllTvPollStateForDebug`/`seedOneNewCreditForDebug`).
- Verification: `CollectionNotificationPollerTest` (commonTest, 6 cases mirroring
  `PersonCreditNotificationPollerTest`), `FollowCollectionButtonUiTest`, `FavoriteCollectionsTabUiTest`,
  plus additions to `CollectionDetailScreenModelTest`/`CollectionDetailScreenUiTest`. All green
  (`desktopTest`, `ktlintCheck`, `assembleDebug`). Real-device confirmation of the notification
  itself not yet done - see `run-app` skill's debug-row steps, same as 3a/3b.

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

## 12. Animated Splash Screen — DONE (2026-08-27)
**Design source**: the MyWatchList Logo design file -
https://claude.ai/design/p/64719451-d56e-493e-b87e-c7dfc863c6cc?file=MyWatchList+Logo.dc.html&via=share
(see [[design-artefact-links]] memory) - read in full via the `claude_design` MCP
(`DesignSync`/`get_file` on `MyWatchList Logo.dc.html`, projectId `64719451-d56e-493e-b87e-c7dfc863c6cc`,
type `PROJECT_TYPE_PROJECT`) after a plain `WebFetch` 403'd on the auth-gated `claude.ai/design/...`
URL.

**The "3b" animation ("Reel spin-up")**, exact spec pulled from the file's `<style>`/markup - this
is authoritative, no need to re-open the design file at implementation time:
- **Icon** (the mint rounded-square M-monogram glyph, 76×76 in a 100×100 viewBox, `rx=18`, fill
  `#5BFFA1`, glyph path fill `#0d0e12`): animates in via `mwlB-reel`, 0.85s
  `cubic-bezier(.2,.9,.2,1)`, `from { opacity:0; transform:rotate(-14deg) scale(.8) }` through a
  60%-keyframe overshoot `{ opacity:1; transform:rotate(4deg) scale(1.04) }` settling to
  `{ opacity:1; transform:rotate(0) scale(1) }`.
- **Sprocket holes** (two vertical cutout strips at x=18/x=74, each a column of seven rounded-rect
  "holes", `#0d0e12` at opacity `.22`, clipped to `x=18/74,y=20,w=8,h=60,rx=3`): continuously roll
  the whole time via `mwl-roll` (`translateY(0)` → `translateY(-24px)`), 0.3s linear infinite - left
  strip plays forward, right strip plays the same keyframes in `reverse`.
- **Wordmark "MyWatch"** (Sora 800, `-.02em` letter-spacing at rest, color inherits page text
  `#e8eaf0`): `mwlB-fade`, 0.7s `ease-out`, delay 0.5s - `from { opacity:0; letter-spacing:.18em }`
  to `{ opacity:1; letter-spacing:-.02em }` (starts wide-tracked and tightens as it fades in).
- **Wordmark "List"** (same face/weight, color `#5BFFA1`): `mwlB-drop`, 0.7s
  `cubic-bezier(.2,.9,.2,1)`, delay 0.85s - drops from `translateY(-38px)` opacity 0, overshoots
  slightly past rest at the 70% keyframe (`translateY(4px)` opacity 1), settles to `translateY(0)`.
- **Total sequence**: icon settles ~0.85s in; "MyWatch" fade completes ~1.2s; "List" drop completes
  ~1.55s - call it a ~1.6s hero beat before whatever gates the splash's dismissal (see below).
  Background in the design file is `radial-gradient(circle at 50% 42%, #15211a, #0d0e12)` on
  near-black `#0d0e12` - matches this app's actual dark M3 tokens closely
  (`md_theme_dark_background = #191C19`, `md_theme_dark_surface = #111411`) but isn't identical;
  reconcile against the real tokens (`theme/Color.kt`) rather than hardcoding the design file's
  literal hex values, same as every other design-artifact-to-app pass in this project.
- Compare/contrast in the source file for context (not being requested): "3a" (Feed & settle - both
  icon halves slide in from opposite sides) and "3c" (Projector wipe - horizontal lockup revealed by
  a clip-path mask plus a full-bleed mint flash) sit alongside 3b as the other two options shown;
  3b is the one explicitly chosen.

**Implementation** (confirmed with the user: fixed ~1.8s dismiss, not gated on data load; native
pre-launch frame where a platform genuinely has one, shared Compose splash everywhere):
- [x] `core/ui/splash/SplashScreen.kt` (commonMain) - Compose `Animatable`/`tween` sequence
  reproducing the icon rotate/scale/opacity reveal (`CubicBezierEasing(0.2f,0.9f,0.2f,1f)` matching
  the design file's own curve) and the two-part wordmark reveal, via a small `keyframeValue()`
  helper that linearly interpolates between the design file's percentage-keyframe stops once
  `progress` itself has been eased - the same two-step model a browser uses to resolve a CSS
  keyframe animation. Colors from `theme/Color.kt`'s real dark tokens, not the design file's literal
  hex; typography is the app's own `FontFamily.Default`, not the design file's Sora (not bundled
  anywhere else in this app). **Simplification, not yet built**: the moving sprocket-hole strips -
  they're baked as static cutouts into the reused `app_icon` drawable already, and an animated
  overlay in exact registration with that baked-in artwork isn't something buildable with confidence
  without an on-device visual check this environment can't do; the icon's own reveal and the
  wordmark drop are 3b's primary identity and are reproduced in full. `isReducedMotionEnabled()`
  (new `expect`/`actual` in `PlatformUtil.kt`, mirrors `isDebugBuild()`'s shape) skips straight to
  the settled end-state - real on Android (`ANIMATOR_DURATION_SCALE`), iOS
  (`UIAccessibilityIsReduceMotionEnabled`), and JS (`prefers-reduced-motion` media query); `false`
  on desktop, no JVM-wide equivalent exists. Wired into `App()` (`App.kt`) ahead of `MainAppScreen`.
- [x] **Android native pre-splash, superseded (2026-08-27) by a native-*animated* splash**: the
  original pass (`androidx.core:core-splashscreen` + `Theme.MyWatchList.Splash` +
  `installSplashScreen()`) showed a static icon natively, then handed off to the Compose
  `SplashScreen` above for the animation - visually two splashes back-to-back, confusing on-device.
  Fixed by moving the "3b" icon reveal (rotate/scale, plus a one-shot sprocket-hole pull) into the
  native splash itself, as a real `AnimatedVectorDrawable`
  (`res/drawable/splash_icon_animated.xml`, targeting `res/drawable/splash_icon.xml` - hand-converted
  from the design team's layered SVG export, `mwl_splash_icon_288_layered.svg`, since AVD requires
  vector path data, not the PNG `app_icon`), set as `windowSplashScreenAnimatedIcon` with
  `windowSplashScreenAnimationDuration=850` (`values/styles.xml`). `App()` (`App.kt`) skips the
  Compose `SplashScreen` entirely on Android (`usesNativeAnimatedSplash()`, new `expect`/`actual`,
  true only on Android) - Android now shows exactly one splash. iOS/Desktop/JS are unaffected (no
  native animated-icon API to hand off to on any of them) and keep the Compose splash, wordmark
  included, since the native icon-only API has no surface for text (see below, though - Android
  gets a static version of it another way). The native AVD has no looping sprocket roll (one-shot
  only, per the SVG export's own conversion notes) and no icon fade-in (`<group>` has no alpha
  property in VectorDrawable) - both stay Compose-only polish on the other three platforms.
  - `AppActivity.onCreate()` originally held the native splash on screen via
    `setKeepOnScreenCondition` for the full 850ms so the animation couldn't get cut short by an
    early Compose first-draw - **reverted (2026-08-27)**: this caused a blank splash on some fast
    force-kill-then-relaunch cycles (confirmed on the `sdk_gphone16k_arm64` emulator specifically,
    not the Galaxy S24 physical device - see the `SurfaceView`-based icon rendering note below).
    `installSplashScreen()` now dismisses at Compose's default first-draw timing again; the icon
    may occasionally get cut short on a very fast device, a smaller cost than a blank launch.
  - **Wordmark ("MyWatch"/"List")**: added via `windowSplashScreenBrandingImage`
    (`res/values-v31/styles.xml`) - this is a real platform-only SplashScreen attribute
    (`android:windowSplashScreenBrandingImage`, API 31+) that `androidx.core:core-splashscreen`'s
    compat theme does not expose at all below that level (confirmed against the library's own
    `attrs.xml` - only `Background`/`AnimatedIcon`/`AnimationDuration`/`IconBackgroundColor`
    exist there), so it lives in a `values-v31` override rather than the base
    `Theme.MyWatchList.SplashBase`, and devices on API 24-30 fall back to icon-only, same as
    before. `res/drawable-xhdpi/splash_branding_asset.png` is a rasterized PNG (not a vector -
    there's no text-layout primitive in `<vector>`/AVD path data), rendered with
    `java.awt.Graphics2D` (`SansSerif` Bold, the app's own
    `md_theme_dark_onBackground`/`onPrimaryContainer` colors, not a design-file asset) since no
    image-editing tool was available in the dev environment. Static only, like the icon - no
    fade/drop animation, unlike the Compose splash's wordmark reveal.
    - `windowSplashScreenBrandingImage` stretches whatever drawable it's pointed at to fill the
      slot's full width by default - fixed by wrapping the PNG in `res/drawable/splash_branding.xml`
      (a `<bitmap>` with `android:gravity="center"`, referencing the PNG renamed to
      `splash_branding_asset`) so the platform draws it at intrinsic size instead. That alone just
      traded stretching for cropping, though (a `<bitmap>` has no fit-to-bounds scaling, only
      positioning) - the actual fix was shrinking the source PNG itself (fontSize 96 to 48, ~358px
      wide at xhdpi = ~179dp) to fit inside the branding slot's real width (~200dp) at intrinsic
      size with no scaling needed either way.
  - **Known emulator-only flakiness**: `windowSplashScreenAnimatedIcon` renders through a
    dedicated `SurfaceView` (confirmed in logcat: `Creating surface for consumer ... SurfaceView
    [Splash Screen ...]`), which has documented blank-frame timing quirks on software-rendered
    emulator GPU paths. Reproduced intermittently on `sdk_gphone16k_arm64` (force-kill via Settings
    "App info" then reopen); could **not** be reproduced across 10+ consecutive kill/relaunch
    cycles on a real Galaxy S24 (SM-S921B, API 36). Left as-is - real devices are unaffected.
- [x] **iOS native pre-launch screen** (was missing entirely - no `UILaunchScreen`/
  `UILaunchStoryboardName` key existed in `Info.plist` before this): added a `UILaunchScreen`
  Info.plist dict (the modern, storyboard-free mechanism) pointing at new `LaunchIcon`/
  `LaunchBackground` asset-catalog entries (`Assets.xcassets`) - static only, since Apple doesn't
  allow custom animation during the traditional launch screen. Confirmed working end-to-end on the
  iPhone 15 Pro Max simulator (2026-08-27): native launch screen -> Compose `SplashScreen` (icon +
  wordmark reveal) -> `MainAppScreen`, no gaps.
  - **Fixed (2026-08-27)**: `LaunchIcon` initially rendered stretched full-bleed across the whole
    screen instead of small and centered - `LaunchIcon.png` had been copied straight from
    `AppIcon.appiconset/AppIcon-1024.png` (1024x1024px) with no `scale` in its `Contents.json`.
    Two things were needed: (1) the source PNG itself had to be resized down to its actual intended
    display size (180x180px, via `sips -Z 180`) - `UIImageName` renders at literal pixel-size-as-
    points with no retina-aware scale lookup the way normal `UIImage(named:)` loading does, so a
    1024px source is 1024pt on screen regardless of any declared `scale` metadata (confirmed by
    testing `scale:3x` in isolation first - zero visual effect); and (2) after resizing the file,
    the simulator kept showing the *old* stretched render even through a clean `xcodebuild` +
    uninstall/reinstall cycle - iOS caches the compiled launch-screen render at the system
    (SpringBoard) level per bundle ID and does not reliably invalidate it on reinstall alone; a full
    `xcrun simctl shutdown` + `boot` of the simulator was required to see the fix take effect. Worth
    remembering for any future launch-screen asset change: reinstalling is not enough to verify one
    on a simulator that already ran an earlier build.
- [x] **Desktop/JS**: no native pre-load mechanism wired up - Compose Desktop's `nativeDistributions`
  DSL has no clean hook for the JVM's `SplashScreen`-image mechanism, and Wasm/JS has no true browser
  splash API (just the `index.html`-static-loader convention). Both get the shared Compose
  `SplashScreen` as their first rendered content and nothing more; a JVM splash image or a static
  web loader remain possible, separate future enhancements, not built here.
- [x] Test: `SplashScreenUiTest` (Compose UI test) - wordmark renders; `onFinished` fires after a
  short injected `durationMillis`.

## 13. Small Increments (Quick Wins)
Backlog for small, self-contained polish items - each one scoped small enough not to need its own
full OAS-endpoints/implementation-checklist writeup ahead of time; add detail once one is actually
picked up.

### 13.1. Show the resolved region on the detail screen's "Where to watch" section — DONE (2026-08-27)
**Goal**: [[feature-region-selector-done]] added a user-selectable region (`AccountScreen`'s
"Region"/"Default fallback region" rows, `RegionRepository`) that drives which watch-provider list
`MovieHeroSection`/`TvHeroSection`'s "Where to watch" resolves against
(`WatchProvidersResponse?.resolveRegion()`, `MovieHeroFacts.kt`) - but the detail screen never shows
*which* region that list came from. A user with an unfamiliar/empty-looking provider list (e.g. the
fallback region kicked in, not their selected one) has no way to tell why without opening Account
settings and checking.

**Implementation**:
- [x] `WatchProvidersResponse?.resolveRegionCode()` (`MovieHeroFacts.kt`) - a sibling to the
  existing `resolveRegion()`, same selected-then-fallback-then-any priority, but returning just the
  region code that won so the UI can label it.
- [x] `ResolvedRegionLabel(regionCode, color)` (`core/ui/hero/HeroComponents.kt`, shared since both
  Movie and TV detail screens need it) - renders `"🇺🇸 US"` via the existing
  `toRegionFlagEmoji()`. **Simplified from the original plan**: code + flag only, not the full
  region name ("United States") or an explicit "(fallback)" label - a full name would need
  `RegionRepository.getAvailableRegions()`, a suspend/cached call neither `MovieDetailScreenModel`
  nor `TvDetailScreenModel` otherwise depends on, judged out of scope for a quick win.
- [x] Wired into `MovieMetaSection.kt`'s `WhereToWatchSection` (trailing label next to the "Where to
  watch" header) and `TvHeroSection.kt` (below the hero's provider-chip row, since TV has no
  separate "Where to watch" section the way Movie does - just the inline hero chips).
- [x] Verified on the desktop build: Movie (`Under Siege`, 1992) shows "🇺🇸 US" next to the header;
  TV (`Reacher`) shows "🇮🇳 IN" under the chips. Confirmed the label correctly stays hidden when a
  title has no watch-provider data at all (e.g. an unreleased movie) - same as the section itself
  already did before this change.

### 13.2. IMDb link on every detail screen + long-press-to-copy on detail titles — DONE
**Goal**: "We have it already" turned out to be true for 3 of 5 detail screens - Movie
(`MovieMetaSection.kt`'s `MovieExternalLinks`), Person (`PersonDetailScreen.kt`'s `PersonLinksRow`)
and Episode (`EpisodeDetailScreen.kt`'s plain "View on IMDb" text) already rendered an IMDb link
from `ExternalIds.imdbId`, which was already being fetched for every one of these models. The two
real gaps, found by grepping every detail model/screen rather than trusting the claim: **TV show**
and **season detail** - both already had `externalIds` fetched and modeled, just nothing rendered
it.

- [x] **TV show**: `TvMetaSection.kt` gained `TvExternalLinks` - an `AssistChip` row (IMDb/
  Instagram/X/Facebook), copy-pasted from `MovieExternalLinks`' exact shape (no Homepage entry -
  `TvDetail` has no `homepage` field).
- [x] **Season detail** (`EpisodeListScreen.kt`, the season's episode list - there's no separate
  "season detail" screen; `AllSeasonsScreen` only lists seasons): a "View on IMDb" header item
  above the episode list, copy-pasted from `EpisodeDetailScreen`'s existing plain-clickable-`Text`
  pattern and reusing its `action_view_on_imdb` string resource, gated on
  `season.externalIds?.imdbId`.
- [x] Both reuse the app's existing `internal expect fun openUrl(url: String?)` (`App.kt`) - the
  same mechanism every other external link in this app already calls. No new URL-opening
  mechanism introduced.
- [x] **Long-press-to-copy on every detail screen's title** (net-new pattern - grepped
  `combinedClickable`/`onLongClick`/clipboard across the whole app first, found nothing to reuse).
  New `Modifier.longPressToCopy(text)` (`core/ui/LongPressToCopy.kt`) - copies to the clipboard via
  `LocalClipboardManager` plus a haptic tick, no toast/snackbar (none exists anywhere in this app;
  adding one app-wide was judged out of scope for a copy-the-title convenience). Deliberately
  **not** Compose 1.11.1's newer suspend `LocalClipboard`/`ClipEntry` API - `ClipEntry`
  construction is platform-native with no shared plain-text constructor, which would mean a new
  `expect`/`actual` per platform just to copy a string; `ClipboardManager` is `@Deprecated` in this
  version but still fully functional and already multiplatform - revisit if it's ever actually
  removed. Wired into all 4 places a detail screen's title can render: the three hero title `Text`s
  (`MovieHeroSection`/`TvHeroSection`/`PersonHeroSection`, visible immediately on load) and the
  shared `DetailTopBar`'s title `Text` (covers Episode/Season/Collection, whose only title lives
  there, and also becomes available on Movie/TV/Person once scrolled past the hero).
- Verification: `LongPressToCopyUiTest` (long-press copies, plain click doesn't), plus
  IMDb-chip/link show/hide cases added to the existing
  `TvDetailScreenUiTest`/`EpisodeListScreenUiTest` files. All green (`desktopTest`, `ktlintCheck`,
  `assembleDebug`). Movie/Person/Episode's own pre-existing IMDb links remain untested (a
  pre-existing gap, not introduced here) - not retrofitted, out of scope for this pass.
- **Fixed 2026-09-12: `LongPressToCopyUiTest` failed in CI.** The original version read
  `LocalClipboardManager.current` and asserted against the real desktop-actual AWT system
  clipboard; that's unavailable on the headless GitHub Actions runner (no X server), so both
  assertions failed there despite passing locally. Fixed by injecting an in-memory fake
  `ClipboardManager` via `CompositionLocalProvider` instead, making the test hermetic - no CI
  workflow change needed.
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
  [item 3c](#3c-new-movie-added-to-a-favorited-collection) - if that ships, followed franchises
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
  [item 8](#8-genre-based-discovery-screen--done-2026-08-18)) - `with_genres`,
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
  [item 6](#6-account-favorites--watchlist-replacing-my-fav-placeholder--done-2026-08-16)).
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
here. [Item 3](#3-local-notifications-returning-series-favorite-actors-favorite-collections)
already proved the pattern: `EpisodeNotificationWorker` runs the pollers from a background worker
with no Activity, no Compose, and no DI container (repositories are constructed directly, and even
`Res.string.*` resolves via `getString` off the main thread). An `AppFunctionService` is the same
shape of caller. Most of the tracked data is also in local SQLite already
([item 2](#2-local-sqlite-database-for-favoriteswatchlist-local-notification-data-source)), so
read functions answer offline and in milliseconds - exactly what an agent needs.

**Candidate functions to expose (start with reads)**

| Function | Backed by | Notes |
|---|---|---|
| `searchTitles(query, mediaType?)` | `SearchRepository` | The safest first function - read-only, no auth, already paginated. |
| `getWatchlist()` / `getFavorites()` | `TrackedMediaRepository` | Local SQLite, offline, no TMDB round-trip. |
| `getUpcomingEpisodes()` | `TrackedMediaRepository.trackedTvForPolling` + `TvRepository` | Reuses item 3a's existing poll state. |
| `whereToWatch(title)` | `MovieRepository`/`TvRepository` + `resolveRegion()` | Reuses the region resolution from [item 11](#11-region-selector-driving-ott-availability--done-2026-08-17). |
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
