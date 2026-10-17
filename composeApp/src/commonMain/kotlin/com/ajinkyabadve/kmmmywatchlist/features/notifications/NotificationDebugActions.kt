package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationLedgerRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationLedgerRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationReason
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepositoryImpl

/**
 * The debug-build "Poll … notifications now" rows in Settings. Each one forces its poller to find
 * something new again, so the row can be re-tapped for a fresh notification on demand instead of
 * being blocked by whatever a previous real poll already recorded.
 *
 * An interface so `AccountScreenModel` can be unit tested without the pollers' network calls.
 */
interface NotificationDebugActions {
    suspend fun pollEpisodeNotificationsNow()

    suspend fun pollPersonNotificationsNow()

    suspend fun pollCollectionNotificationsNow()
}

class NotificationDebugActionsImpl(
    private val trackedMediaRepository: TrackedMediaRepository = TrackedMediaRepositoryImpl(),
    private val favoritePersonRepository: FavoritePersonRepository = FavoritePersonRepositoryImpl(),
    private val favoriteCollectionRepository: FavoriteCollectionRepository = FavoriteCollectionRepositoryImpl(),
    private val notificationLedgerRepository: NotificationLedgerRepository = NotificationLedgerRepositoryImpl(),
) : NotificationDebugActions {
    /**
     * Makes the exact next episode every tracked returning series currently has look
     * newly-announced again. Scoped to episode reasons only (clearForReasonForDebug), not the
     * whole ledger, so this never disturbs the person or collection rows below.
     */
    override suspend fun pollEpisodeNotificationsNow() {
        trackedMediaRepository.resetAllTvPollStateForDebug()
        notificationLedgerRepository.clearForReasonForDebug(NotificationReason.EPISODE_ANNOUNCED)
        notificationLedgerRepository.clearForReasonForDebug(NotificationReason.EPISODE_AIRING)
        TvEpisodeNotificationPoller(
            trackedMediaRepository = trackedMediaRepository,
            notificationLedgerRepository = notificationLedgerRepository,
        ).poll()
    }

    /**
     * Item 3b, kept separate (requested 2026-08-27) so testing person-credit notifications never
     * resets or races with episode testing. seedOneNewCreditForDebug (not a blanket credit-state
     * reset) holds back exactly one of each favorited person's current credits, so the poll
     * notifies once per person, not once per credit - a blanket reset floods anyone with a real
     * filmography.
     */
    override suspend fun pollPersonNotificationsNow() {
        notificationLedgerRepository.clearForReasonForDebug(NotificationReason.PERSON_NEW_CREDIT)
        val poller =
            PersonCreditNotificationPoller(
                favoritePersonRepository = favoritePersonRepository,
                notificationLedgerRepository = notificationLedgerRepository,
            )
        poller.seedOneNewCreditForDebug()
        poller.poll()
    }

    /** Item 3c - same idea as [pollPersonNotificationsNow], one new part per followed collection. */
    override suspend fun pollCollectionNotificationsNow() {
        notificationLedgerRepository.clearForReasonForDebug(NotificationReason.COLLECTION_NEW_PART)
        val poller =
            CollectionNotificationPoller(
                favoriteCollectionRepository = favoriteCollectionRepository,
                notificationLedgerRepository = notificationLedgerRepository,
            )
        poller.seedOneNewPartForDebug()
        poller.poll()
    }
}
