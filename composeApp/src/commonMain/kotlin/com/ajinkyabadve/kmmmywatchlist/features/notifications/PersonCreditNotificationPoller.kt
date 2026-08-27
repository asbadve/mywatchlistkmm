package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.notification.LocalNotifier
import com.ajinkyabadve.kmmmywatchlist.core.notification.PersonNotificationTarget
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationLedgerRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationLedgerRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationReason
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCredit
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonPollCandidate
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.notification_person_new_credit_body
import mywatchlist.composeapp.generated.resources.notification_person_new_credit_title
import org.jetbrains.compose.resources.getString

private object PersonCreditNotificationPollerConstant {
    const val TAG = "PersonCreditNotificationPoller"
    const val CREDIT_ID_SEPARATOR = ","

    // Same reasoning as TvEpisodeNotificationPollerConstant's poster sizing - no real screen
    // density to reference from a background poll.
    const val PROFILE_TARGET_WIDTH_DP = 500
    const val PROFILE_DENSITY = 1f
}

/**
 * `future_features_checklist.md` item 3b: polls every favorited person's `combined_credits` for a
 * credit id not seen on the previous poll and posts a local notification for it. Plain class, not
 * a ScreenModel - driven by `NotificationScheduler`'s platform background job, same shape as
 * [TvEpisodeNotificationPoller] (mirrors its per-item try/catch, cursor-always-updates-regardless-
 * of-notify behavior, and [NotificationLedgerRepository]-backed dedup).
 *
 * Every notification navigates to the favorited person's own detail screen on tap
 * ([PersonNotificationTarget]) - Android and iOS only, per [LocalNotifier.post]'s kdoc; Desktop
 * and JS still show the notification, they just don't act on a tap for this notification kind
 * (confirmed 2026-08-26, same reasoning as Desktop's pre-existing TrayIcon click limitation).
 */
class PersonCreditNotificationPoller(
    private val favoritePersonRepository: FavoritePersonRepository = FavoritePersonRepositoryImpl(),
    private val personRepository: PersonRepository = PersonRepositoryImpl(),
    private val notificationLedgerRepository: NotificationLedgerRepository = NotificationLedgerRepositoryImpl(),
    private val notifier: LocalNotifier = LocalNotifier,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    suspend fun poll() {
        favoritePersonRepository.favoritePeopleForPolling().forEach { candidate -> pollOne(candidate) }
    }

    /**
     * Debug-only (see AccountScreen's "Poll person notifications now" row): seeds a baseline that's
     * missing exactly one of this person's *current* credits, for every favorited person, so the
     * very next [poll] notifies once per person - never once per credit. A blanket reset to `""`
     * (this repository's other debug helper,
     * [FavoritePersonRepository.resetAllCreditStateForDebug]) makes every current credit look new
     * at once, which floods anyone with a real filmography; this instead fetches their current
     * credits itself and holds one back, so the diff in the following [poll] call has exactly one
     * "new" credit to find - the same one-notification-per-forced-test-poll shape
     * `resetAllTvPollStateForDebug` already gives TV (a show only ever has one next episode at a
     * time, so no equivalent flood was possible there to begin with).
     */
    suspend fun seedOneNewCreditForDebug() {
        favoritePersonRepository.favoritePeopleForPolling().forEach { candidate ->
            try {
                val detail = personRepository.getPersonDetails(candidate.id)
                val currentIds =
                    (detail.combinedCredits?.cast.orEmpty() + detail.combinedCredits?.crew.orEmpty())
                        .mapNotNull { it.creditId }
                        .toSet()
                val baseline = if (currentIds.size > 1) currentIds - currentIds.first() else emptySet()
                favoritePersonRepository.updateLastKnownCreditIds(
                    candidate.id,
                    baseline.joinToString(PersonCreditNotificationPollerConstant.CREDIT_ID_SEPARATOR),
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

    private suspend fun pollOne(candidate: FavoritePersonPollCandidate) {
        try {
            val detail = personRepository.getPersonDetails(candidate.id)
            val credits = (detail.combinedCredits?.cast.orEmpty() + detail.combinedCredits?.crew.orEmpty())
            val currentIds = credits.mapNotNull { it.creditId }.toSet()

            // First poll for this person (never polled before, lastKnownCreditIds still null):
            // seed the baseline silently instead of diffing against an empty set, which would
            // treat their entire existing filmography as "new" and notify once per credit - a
            // real flood for anyone with a long filmography. Only credits that appear *after*
            // this baseline - i.e. genuinely new since the user followed them - ever notify.
            if (candidate.lastKnownCreditIds == null) {
                if (currentIds.isNotEmpty()) {
                    favoritePersonRepository.updateLastKnownCreditIds(
                        candidate.id,
                        currentIds.joinToString(PersonCreditNotificationPollerConstant.CREDIT_ID_SEPARATOR),
                    )
                }
                return
            }

            val previouslyKnownIds =
                candidate.lastKnownCreditIds
                    .split(PersonCreditNotificationPollerConstant.CREDIT_ID_SEPARATOR)
                    .filter { it.isNotBlank() }
                    .toSet()

            credits
                .filter { credit -> credit.creditId != null && credit.creditId !in previouslyKnownIds }
                .forEach { credit -> notifyIfNew(personId = candidate.id, personName = detail.name, credit = credit) }

            if (currentIds.isNotEmpty()) {
                favoritePersonRepository.updateLastKnownCreditIds(
                    candidate.id,
                    currentIds.joinToString(PersonCreditNotificationPollerConstant.CREDIT_ID_SEPARATOR),
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
        personId: Long,
        personName: String,
        credit: PersonCredit,
    ) {
        val creditId = credit.creditId ?: return
        val mediaType = if (credit.mediaType == MediaTypeConstant.TV) MediaTypeConstant.TV else MediaTypeConstant.MOVIE
        if (notificationLedgerRepository.alreadyNotified(
                personId.toInt(),
                mediaType,
                NotificationReason.PERSON_NEW_CREDIT,
                creditId,
            )
        ) {
            return
        }

        val posterUrl =
            ImageConfigResolver.resolve(
                credit.posterPath,
                ImageConfigResolver.ImageType.POSTER,
                PersonCreditNotificationPollerConstant.PROFILE_TARGET_WIDTH_DP,
                PersonCreditNotificationPollerConstant.PROFILE_DENSITY,
            )
        notifier.post(
            notificationId = Triple(personId, mediaType, creditId).hashCode(),
            title = getString(Res.string.notification_person_new_credit_title, personName),
            body = getString(Res.string.notification_person_new_credit_body, personName, credit.displayTitle),
            deepLink = PersonNotificationTarget(personId),
            posterUrl = posterUrl,
        )
        notificationLedgerRepository.recordNotified(personId.toInt(), mediaType, NotificationReason.PERSON_NEW_CREDIT, creditId, now())
    }

    private fun logPollFailure(
        personId: Long,
        throwable: Throwable,
    ) {
        Napier.e(tag = PersonCreditNotificationPollerConstant.TAG, throwable = throwable) { "Failed to poll personId: $personId" }
    }
}
