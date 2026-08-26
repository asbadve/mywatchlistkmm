package com.ajinkyabadve.kmmmywatchlist.core.notification

/**
 * Posts a local (on-device, no push infra) notification - see `future_features_checklist.md`
 * item 3's shared-infrastructure checklist. Not `@Composable` (unlike `core/auth/WebAuthLauncher.kt`'s
 * `expect`/`actual`s) since posting isn't tied to a composition - the poller that calls this runs
 * on a background scheduler, not inside a screen. Permission *requesting* is a separate
 * [rememberNotificationPermissionRequester] instead, precisely because that part - unlike posting -
 * needs an Activity/composition context on Android.
 */
expect object LocalNotifier {
    /**
     * [notificationId] should be a stable hash of whatever this notification is about (e.g.
     * `Triple(mediaId, mediaType, reason).hashCode()`) so a genuinely new notification about the
     * same thing (a later season's episode, for instance) replaces the old tray entry instead of
     * stacking indefinitely - not because dedup is expected to fail (the caller's own ledger
     * already prevents an exact repeat from getting this far). Silently no-ops if permission was
     * never granted - callers don't need to check first. [deepLink] is what a tap should open -
     * carried through the platform notification itself (extras/`userInfo`) so it survives the app
     * not even running yet, then surfaces via [PendingEpisodeNotificationTarget] once it does.
     * [posterUrl] (episode still, falling back to the show poster - see `TvEpisodeNotificationPoller`)
     * is best-effort: a download/decode failure silently falls back to a text-only notification
     * rather than losing the notification entirely, so this never throws on a bad/unreachable URL.
     * `suspend` (unlike the rest of this `expect`/`actual` pattern's siblings) because fetching that
     * image is itself a network call - every caller already runs inside a coroutine (the poller).
     */
    suspend fun post(
        notificationId: Int,
        title: String,
        body: String,
        deepLink: EpisodeNotificationTarget,
        posterUrl: String?,
    )
}
