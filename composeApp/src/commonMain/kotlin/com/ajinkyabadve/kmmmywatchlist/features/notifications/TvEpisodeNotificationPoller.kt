package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.notification.EpisodeNotificationTarget
import com.ajinkyabadve.kmmmywatchlist.core.notification.LocalNotifier
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationLedgerRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationLedgerRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.notification_episode_airing_body
import mywatchlist.composeapp.generated.resources.notification_episode_airing_title
import mywatchlist.composeapp.generated.resources.notification_episode_announced_body
import mywatchlist.composeapp.generated.resources.notification_episode_announced_title
import org.jetbrains.compose.resources.getString

private object TvEpisodeNotificationPollerConstant {
    const val TAG = "TvEpisodeNotificationPoller"
    const val ENDED = "Ended"
    const val CANCELED = "Canceled"
    const val REASON_ANNOUNCED = "episode_announced"
    const val REASON_AIRING = "episode_airing"

    // No real screen density to reference from a background poll - a fixed target roughly matched
    // to how large a notification's expanded image actually renders is close enough.
    const val POSTER_TARGET_WIDTH_DP = 500
    const val POSTER_DENSITY = 1f
}

/**
 * `future_features_checklist.md` item 3a: polls every favorited/watchlisted TV show for a new or
 * newly-airing episode and posts a local notification. Plain class, not a ScreenModel - nothing
 * composes this, it's driven by `NotificationScheduler`'s platform background job.
 *
 * Two distinct notification reasons per show, per the confirmed dedup rule (one notification per
 * (media id, reason), not one ever per media id): [TvEpisodeNotificationPollerConstant.REASON_ANNOUNCED]
 * fires once when a new episode's air date first becomes known, and
 * [TvEpisodeNotificationPollerConstant.REASON_AIRING] fires once, separately, on the air date
 * itself - [notificationLedgerRepository] is what makes each of those idempotent per exact air
 * date while still letting a later season's new episode notify again.
 */
class TvEpisodeNotificationPoller(
    private val trackedMediaRepository: TrackedMediaRepository = TrackedMediaRepositoryImpl(),
    private val tvRepository: TvRepository = TvRepositoryImpl(),
    private val notificationLedgerRepository: NotificationLedgerRepository = NotificationLedgerRepositoryImpl(),
    private val notifier: LocalNotifier = LocalNotifier,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val today: () -> String = { Clock.System.todayIn(TimeZone.currentSystemDefault()).toString() },
) {
    suspend fun poll() {
        val today = today()
        trackedMediaRepository.trackedTvForPolling().forEach { candidate ->
            // A show already known to be over ages out of polling entirely - zero network calls
            // for it on every subsequent poll, not just a skipped notification.
            if (candidate.lastKnownStatus == TvEpisodeNotificationPollerConstant.ENDED ||
                candidate.lastKnownStatus == TvEpisodeNotificationPollerConstant.CANCELED
            ) {
                return@forEach
            }
            pollOne(candidate.id, candidate.lastKnownNextEpisodeAirDate, today)
        }
    }

    private suspend fun pollOne(
        id: Int,
        lastKnownAirDate: String?,
        today: String,
    ) {
        try {
            val detail = tvRepository.getTvDetails(id.toLong())
            trackedMediaRepository.updateLastKnownStatusForMediaType(id, MediaTypeConstant.TV, detail.status)

            val nextEpisode = detail.nextEpisodeToAir
            val airDate = nextEpisode?.airDate
            val posterUrl =
                if (nextEpisode?.stillPath != null) {
                    ImageConfigResolver.resolve(
                        nextEpisode.stillPath,
                        ImageConfigResolver.ImageType.STILL,
                        TvEpisodeNotificationPollerConstant.POSTER_TARGET_WIDTH_DP,
                        TvEpisodeNotificationPollerConstant.POSTER_DENSITY,
                    )
                } else {
                    ImageConfigResolver.resolve(
                        detail.posterPath,
                        ImageConfigResolver.ImageType.POSTER,
                        TvEpisodeNotificationPollerConstant.POSTER_TARGET_WIDTH_DP,
                        TvEpisodeNotificationPollerConstant.POSTER_DENSITY,
                    )
                }
            if (airDate != null && nextEpisode != null && airDate != lastKnownAirDate) {
                notifyIfNew(
                    id = id,
                    reason = TvEpisodeNotificationPollerConstant.REASON_ANNOUNCED,
                    cursorValue = airDate,
                    title = getString(Res.string.notification_episode_announced_title),
                    body = getString(Res.string.notification_episode_announced_body, detail.title, airDate),
                    deepLink = EpisodeNotificationTarget(id.toLong(), nextEpisode.seasonNumber, nextEpisode.episodeNumber),
                    posterUrl = posterUrl,
                )
                trackedMediaRepository.updatePollStateForMediaType(id, MediaTypeConstant.TV, airDate)
            }
            if (airDate != null && nextEpisode != null && airDate == today) {
                notifyIfNew(
                    id = id,
                    reason = TvEpisodeNotificationPollerConstant.REASON_AIRING,
                    cursorValue = airDate,
                    title = getString(Res.string.notification_episode_airing_title),
                    body = getString(Res.string.notification_episode_airing_body, detail.title),
                    deepLink = EpisodeNotificationTarget(id.toLong(), nextEpisode.seasonNumber, nextEpisode.episodeNumber),
                    posterUrl = posterUrl,
                )
            }
        } catch (e: HttpExceptions) {
            logPollFailure(id, e)
        } catch (e: IOException) {
            logPollFailure(id, e)
        } catch (e: ContentConvertException) {
            logPollFailure(id, e)
        } catch (e: SerializationException) {
            logPollFailure(id, e)
        }
    }

    private suspend fun notifyIfNew(
        id: Int,
        reason: String,
        cursorValue: String,
        title: String,
        body: String,
        deepLink: EpisodeNotificationTarget,
        posterUrl: String?,
    ) {
        if (notificationLedgerRepository.alreadyNotified(id, MediaTypeConstant.TV, reason, cursorValue)) return
        notifier.post(Triple(id, MediaTypeConstant.TV, reason).hashCode(), title, body, deepLink, posterUrl)
        notificationLedgerRepository.recordNotified(id, MediaTypeConstant.TV, reason, cursorValue, now())
    }

    private fun logPollFailure(
        id: Int,
        throwable: Throwable,
    ) {
        Napier.e(tag = TvEpisodeNotificationPollerConstant.TAG, throwable = throwable) { "Failed to poll tvId: $id" }
    }
}
