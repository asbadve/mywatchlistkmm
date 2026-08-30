package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeFavoriteCollectionRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.FakeNotificationLedgerRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationReason
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private object CollectionNotificationPollerTestConstant {
    const val COLLECTION_ID = 801L
    const val COLLECTION_NAME = "Fake Favorite Franchise"
}

class CollectionNotificationPollerTest {
    private fun buildPoller(
        favoriteCollectionRepository: FakeFavoriteCollectionRepository,
        movieRepository: MovieRepository,
        notificationLedgerRepository: FakeNotificationLedgerRepository,
    ) = CollectionNotificationPoller(
        favoriteCollectionRepository = favoriteCollectionRepository,
        movieRepository = movieRepository,
        notificationLedgerRepository = notificationLedgerRepository,
        now = { 0L },
    )

    /** The first poll after following a collection must never notify for its existing parts - only
     *  baseline it - or following a long-running franchise would flood the user with one
     *  notification per existing film. */
    @Test
    fun testFirstPollAfterFollowingSeedsBaselineWithoutNotifying() =
        runTest {
            val favoriteCollectionRepository = FakeFavoriteCollectionRepository()
            favoriteCollectionRepository.seedFavorite(CollectionNotificationPollerTestConstant.COLLECTION_ID)
            val movieRepository = FakeMovieRepository()
            movieRepository.getCollectionDetailsResult =
                Result.success(
                    CollectionDetail(
                        id = CollectionNotificationPollerTestConstant.COLLECTION_ID,
                        name = CollectionNotificationPollerTestConstant.COLLECTION_NAME,
                        parts = listOf(Movie(id = 1), Movie(id = 2), Movie(id = 3)),
                    ),
                )
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoriteCollectionRepository, movieRepository, notificationLedgerRepository)

            poller.poll()

            assertTrue(notificationLedgerRepository.recordNotifiedCalls.isEmpty())
            assertEquals(
                listOf(CollectionNotificationPollerTestConstant.COLLECTION_ID to "1,2,3"),
                favoriteCollectionRepository.updateLastKnownPartIdsCalls,
            )
        }

    @Test
    fun testPartAddedAfterBaselineNotifiesOnce() =
        runTest {
            val favoriteCollectionRepository = FakeFavoriteCollectionRepository()
            favoriteCollectionRepository.seedFavorite(CollectionNotificationPollerTestConstant.COLLECTION_ID, lastKnownPartIds = "1")
            val movieRepository = FakeMovieRepository()
            movieRepository.getCollectionDetailsResult =
                Result.success(
                    CollectionDetail(
                        id = CollectionNotificationPollerTestConstant.COLLECTION_ID,
                        name = CollectionNotificationPollerTestConstant.COLLECTION_NAME,
                        parts = listOf(Movie(id = 1), Movie(id = 2)),
                    ),
                )
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoriteCollectionRepository, movieRepository, notificationLedgerRepository)

            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
            assertEquals(NotificationReason.COLLECTION_NEW_PART, notificationLedgerRepository.recordNotifiedCalls.single().reason)
        }

    @Test
    fun testRepollingSamePartsDoesNotReNotify() =
        runTest {
            val favoriteCollectionRepository = FakeFavoriteCollectionRepository()
            favoriteCollectionRepository.seedFavorite(CollectionNotificationPollerTestConstant.COLLECTION_ID, lastKnownPartIds = "1")
            val movieRepository = FakeMovieRepository()
            movieRepository.getCollectionDetailsResult =
                Result.success(
                    CollectionDetail(
                        id = CollectionNotificationPollerTestConstant.COLLECTION_ID,
                        name = CollectionNotificationPollerTestConstant.COLLECTION_NAME,
                        parts = listOf(Movie(id = 1), Movie(id = 2)),
                    ),
                )
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoriteCollectionRepository, movieRepository, notificationLedgerRepository)

            poller.poll()
            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
        }

    @Test
    fun testASecondNewPartNotifiesAgain() =
        runTest {
            val favoriteCollectionRepository = FakeFavoriteCollectionRepository()
            favoriteCollectionRepository.seedFavorite(CollectionNotificationPollerTestConstant.COLLECTION_ID, lastKnownPartIds = "1")
            val movieRepository = FakeMovieRepository()
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoriteCollectionRepository, movieRepository, notificationLedgerRepository)

            movieRepository.getCollectionDetailsResult =
                Result.success(
                    CollectionDetail(
                        id = CollectionNotificationPollerTestConstant.COLLECTION_ID,
                        name = CollectionNotificationPollerTestConstant.COLLECTION_NAME,
                        parts = listOf(Movie(id = 1), Movie(id = 2)),
                    ),
                )
            poller.poll()

            movieRepository.getCollectionDetailsResult =
                Result.success(
                    CollectionDetail(
                        id = CollectionNotificationPollerTestConstant.COLLECTION_ID,
                        name = CollectionNotificationPollerTestConstant.COLLECTION_NAME,
                        parts = listOf(Movie(id = 1), Movie(id = 2), Movie(id = 3)),
                    ),
                )
            poller.poll()

            assertEquals(2, notificationLedgerRepository.recordNotifiedCalls.size)
        }

    @Test
    fun testPerCollectionExceptionDoesNotStopPollingTheRestOfTheBatch() =
        runTest {
            val failingId = 1L
            val healthyId = 2L
            val favoriteCollectionRepository = FakeFavoriteCollectionRepository()
            favoriteCollectionRepository.seedFavorite(failingId, lastKnownPartIds = "")
            favoriteCollectionRepository.seedFavorite(healthyId, lastKnownPartIds = "")
            val movieRepository =
                object : MovieRepository by FakeMovieRepository() {
                    override suspend fun getCollectionDetails(collectionId: Long): CollectionDetail {
                        if (collectionId == failingId) throw IOException("network down")
                        return CollectionDetail(
                            id = collectionId,
                            name = CollectionNotificationPollerTestConstant.COLLECTION_NAME,
                            parts = listOf(Movie(id = 1)),
                        )
                    }
                }
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoriteCollectionRepository, movieRepository, notificationLedgerRepository)

            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
            assertTrue(notificationLedgerRepository.recordNotifiedCalls.single().id == healthyId.toInt())
        }

    /** The debug "force a test notification" path must notify once per favorited collection, not
     *  once per part - unlike a blanket reset to "", which would flood a long-running franchise. */
    @Test
    fun testSeedOneNewPartForDebugThenPollNotifiesExactlyOncePerCollection() =
        runTest {
            val favoriteCollectionRepository = FakeFavoriteCollectionRepository()
            favoriteCollectionRepository.seedFavorite(CollectionNotificationPollerTestConstant.COLLECTION_ID, lastKnownPartIds = "1")
            val movieRepository = FakeMovieRepository()
            movieRepository.getCollectionDetailsResult =
                Result.success(
                    CollectionDetail(
                        id = CollectionNotificationPollerTestConstant.COLLECTION_ID,
                        name = CollectionNotificationPollerTestConstant.COLLECTION_NAME,
                        parts = listOf(Movie(id = 1), Movie(id = 2), Movie(id = 3), Movie(id = 4)),
                    ),
                )
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoriteCollectionRepository, movieRepository, notificationLedgerRepository)

            poller.seedOneNewPartForDebug()
            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
        }
}
