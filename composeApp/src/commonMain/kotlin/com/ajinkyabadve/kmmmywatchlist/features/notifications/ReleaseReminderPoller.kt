package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.SerializationException

private object ReleaseReminderPollerConstant {
    const val TAG = "ReleaseReminderPoller"
}

/**
 * Checklist item 16: keeps each stored reminder's release date fresh and reschedules. It never
 * posts a notification itself - the OS delivers reminders at the chosen time (see
 * [ReleaseReminderCoordinator]), so this poller's unreliable background cadence only affects how
 * quickly a slipped date is noticed, never whether a reminder fires.
 *
 * Run by `NotificationScheduler`'s platform background job next to item 3's pollers - one periodic
 * job, not a separate one, same reasoning as `PersonCreditNotificationPoller`'s kdoc.
 */
class ReleaseReminderPoller(
    private val repository: ReleaseReminderRepository = ReleaseReminderRepositoryImpl(),
    private val movieRepository: MovieRepository = MovieRepositoryImpl(),
    private val tvRepository: TvRepository = TvRepositoryImpl(),
    private val regionRepository: RegionRepository = RegionRepositoryImpl(),
    private val coordinator: ReleaseReminderCoordinator = ReleaseReminderCoordinator(repository),
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) {
    suspend fun poll() {
        repository.deleteExpired(today())
        repository.allReminders().forEach { reminder -> refreshOne(reminder) }
        coordinator.rescheduleAll()
    }

    private suspend fun refreshOne(reminder: ReleaseReminder) {
        try {
            val resolved = resolveLatest(reminder) ?: return
            if (resolved.date != reminder.releaseDate || resolved.source.encode() != reminder.releaseSource) {
                repository.updateReleaseDate(reminder.key, resolved.date, resolved.source.encode())
            }
        } catch (e: HttpExceptions) {
            logPollFailure(reminder, e)
        } catch (e: IOException) {
            logPollFailure(reminder, e)
        } catch (e: ContentConvertException) {
            logPollFailure(reminder, e)
        } catch (e: SerializationException) {
            logPollFailure(reminder, e)
        }
    }

    private suspend fun resolveLatest(reminder: ReleaseReminder): ResolvedRelease? {
        val key = reminder.key
        val seasonNumber = key.seasonNumber
        val episodeNumber = key.episodeNumber
        return when {
            seasonNumber != null && episodeNumber != null -> {
                val episode =
                    tvRepository
                        .getSeasonDetails(key.mediaId, seasonNumber)
                        .episodes
                        .firstOrNull { it.episodeNumber == episodeNumber }
                parseDateOrNull(episode?.airDate)?.let { ResolvedRelease(it, ReleaseSource(ReleaseSourceKind.EPISODE_AIR)) }
            }
            key.mediaType == MediaTypeConstant.TV -> tvRepository.getTvDetails(key.mediaId).resolveReleaseDate()
            else ->
                movieRepository
                    .getMovieDetails(key.mediaId)
                    .resolveReleaseDate(regionRepository.getSelectedRegion(), regionRepository.getFallbackRegion())
        }
    }

    private fun logPollFailure(
        reminder: ReleaseReminder,
        throwable: Throwable,
    ) {
        Napier.e(tag = ReleaseReminderPollerConstant.TAG, throwable = throwable) { "Failed to refresh reminder: ${reminder.key}" }
    }
}
