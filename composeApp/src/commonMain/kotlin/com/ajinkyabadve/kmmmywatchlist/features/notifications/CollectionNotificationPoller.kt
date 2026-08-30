package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.notification.CollectionNotificationTarget
import com.ajinkyabadve.kmmmywatchlist.core.notification.LocalNotifier
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionPollCandidate
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationLedgerRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationLedgerRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationReason
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.notification_collection_new_part_body
import mywatchlist.composeapp.generated.resources.notification_collection_new_part_title
import org.jetbrains.compose.resources.getString

private object CollectionNotificationPollerConstant {
    const val TAG = "CollectionNotificationPoller"
    const val PART_ID_SEPARATOR = ","

    // Same reasoning as PersonCreditNotificationPollerConstant's profile sizing - no real screen
    // density to reference from a background poll.
    const val POSTER_TARGET_WIDTH_DP = 500
    const val POSTER_DENSITY = 1f
}

/**
 * `future_features_checklist.md` item 3c: polls every favorited collection's `parts` for a movie
 * id not seen on the previous poll and posts a local notification for it. Plain class, not a
 * ScreenModel - driven by `NotificationScheduler`'s platform background job, same shape as
 * [PersonCreditNotificationPoller] (mirrors its per-item try/catch, cursor-always-updates-
 * regardless-of-notify behavior, first-poll-seeds-baseline-without-notifying, and
 * [NotificationLedgerRepository]-backed dedup).
 *
 * Every notification navigates to the favorited collection's own detail screen on tap
 * ([CollectionNotificationTarget]) - Android and iOS only, per [LocalNotifier.post]'s kdoc, same
 * scope decision as [PersonNotificationTarget][com.ajinkyabadve.kmmmywatchlist.core.notification.PersonNotificationTarget].
 */
class CollectionNotificationPoller(
    private val favoriteCollectionRepository: FavoriteCollectionRepository = FavoriteCollectionRepositoryImpl(),
    private val movieRepository: MovieRepository = MovieRepositoryImpl(),
    private val notificationLedgerRepository: NotificationLedgerRepository = NotificationLedgerRepositoryImpl(),
    private val notifier: LocalNotifier = LocalNotifier,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    suspend fun poll() {
        favoriteCollectionRepository.favoriteCollectionsForPolling().forEach { candidate -> pollOne(candidate) }
    }

    /**
     * Debug-only (see AccountScreen's "Poll collection notifications now" row): seeds a baseline
     * that's missing exactly one of this collection's *current* parts, for every favorited
     * collection, so the very next [poll] notifies once per collection - never once per part. Same
     * reasoning as [PersonCreditNotificationPoller.seedOneNewCreditForDebug].
     */
    suspend fun seedOneNewPartForDebug() {
        favoriteCollectionRepository.favoriteCollectionsForPolling().forEach { candidate ->
            try {
                val detail = movieRepository.getCollectionDetails(candidate.id)
                val currentIds = detail.parts.map { it.id }.toSet()
                val baseline = if (currentIds.size > 1) currentIds - currentIds.first() else emptySet()
                favoriteCollectionRepository.updateLastKnownPartIds(
                    candidate.id,
                    baseline.joinToString(CollectionNotificationPollerConstant.PART_ID_SEPARATOR),
                )
            } catch (e: HttpExceptions) {
                logPollFailure(candidate.id, e)
            } catch (e: IOException) {
                logPollFailure(candidate.id, e)
            } catch (e: ContentConvertException) {
                logPollFailure(candidate.id, e)
            } catch (e: SerializationException) {
                logPollFailure(candidate.id, e)
            }
        }
    }

    private suspend fun pollOne(candidate: FavoriteCollectionPollCandidate) {
        try {
            val detail = movieRepository.getCollectionDetails(candidate.id)
            val currentIds = detail.parts.map { it.id }.toSet()

            // First poll for this collection (never polled before, lastKnownPartIds still null):
            // seed the baseline silently instead of diffing against an empty set, which would
            // treat every existing film in the franchise as "new" and notify once per part - a
            // real flood for a long-running franchise. Only parts that appear *after* this
            // baseline - i.e. genuinely new since the user followed it - ever notify.
            if (candidate.lastKnownPartIds == null) {
                if (currentIds.isNotEmpty()) {
                    favoriteCollectionRepository.updateLastKnownPartIds(
                        candidate.id,
                        currentIds.joinToString(CollectionNotificationPollerConstant.PART_ID_SEPARATOR) { it.toString() },
                    )
                }
                return
            }

            val previouslyKnownIds =
                candidate.lastKnownPartIds
                    .split(CollectionNotificationPollerConstant.PART_ID_SEPARATOR)
                    .filter { it.isNotBlank() }
                    .mapNotNull { it.toIntOrNull() }
                    .toSet()

            detail.parts
                .filter { part -> part.id !in previouslyKnownIds }
                .forEach { part -> notifyIfNew(collectionId = candidate.id, collectionName = detail.name, part = part) }

            if (currentIds.isNotEmpty()) {
                favoriteCollectionRepository.updateLastKnownPartIds(
                    candidate.id,
                    currentIds.joinToString(CollectionNotificationPollerConstant.PART_ID_SEPARATOR) { it.toString() },
                )
            }
        } catch (e: HttpExceptions) {
            logPollFailure(candidate.id, e)
        } catch (e: IOException) {
            logPollFailure(candidate.id, e)
        } catch (e: ContentConvertException) {
            logPollFailure(candidate.id, e)
        } catch (e: SerializationException) {
            logPollFailure(candidate.id, e)
        }
    }

    private suspend fun notifyIfNew(
        collectionId: Long,
        collectionName: String,
        part: Movie,
    ) {
        val cursorValue = part.id.toString()
        if (notificationLedgerRepository.alreadyNotified(
                collectionId.toInt(),
                MediaTypeConstant.COLLECTION,
                NotificationReason.COLLECTION_NEW_PART,
                cursorValue,
            )
        ) {
            return
        }

        val posterUrl =
            ImageConfigResolver.resolve(
                part.posterPath,
                ImageConfigResolver.ImageType.POSTER,
                CollectionNotificationPollerConstant.POSTER_TARGET_WIDTH_DP,
                CollectionNotificationPollerConstant.POSTER_DENSITY,
            )
        notifier.post(
            notificationId = Triple(collectionId, MediaTypeConstant.COLLECTION, cursorValue).hashCode(),
            title = getString(Res.string.notification_collection_new_part_title, collectionName),
            body = getString(Res.string.notification_collection_new_part_body, part.title, collectionName),
            deepLink = CollectionNotificationTarget(collectionId),
            posterUrl = posterUrl,
        )
        notificationLedgerRepository.recordNotified(
            collectionId.toInt(),
            MediaTypeConstant.COLLECTION,
            NotificationReason.COLLECTION_NEW_PART,
            cursorValue,
            now(),
        )
    }

    private fun logPollFailure(
        collectionId: Long,
        throwable: Throwable,
    ) {
        Napier.e(tag = CollectionNotificationPollerConstant.TAG, throwable = throwable) { "Failed to poll collectionId: $collectionId" }
    }
}
