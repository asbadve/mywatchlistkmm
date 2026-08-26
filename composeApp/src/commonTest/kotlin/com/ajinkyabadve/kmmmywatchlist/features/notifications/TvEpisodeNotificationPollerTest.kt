package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedTvPollCandidate
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.FakeNotificationLedgerRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.FakeTvRepository
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private object TvEpisodeNotificationPollerTestConstant {
    const val TV_ID = 501
    const val SHOW_TITLE = "Fake Returning Series"
    const val TODAY = "2026-08-25"
    const val ANNOUNCED_REASON = "episode_announced"
    const val AIRING_REASON = "episode_airing"
}

class TvEpisodeNotificationPollerTest {
    private fun buildPoller(
        trackedMediaRepository: FakeTrackedMediaRepository,
        tvRepository: TvRepository,
        notificationLedgerRepository: FakeNotificationLedgerRepository,
    ) = TvEpisodeNotificationPoller(
        trackedMediaRepository = trackedMediaRepository,
        tvRepository = tvRepository,
        notificationLedgerRepository = notificationLedgerRepository,
        now = { 0L },
        today = { TvEpisodeNotificationPollerTestConstant.TODAY },
    )

    @Test
    fun testNewAirDateNotifiesOnceUnderAnnouncedReason() =
        runTest {
            val trackedMediaRepository = FakeTrackedMediaRepository()
            trackedMediaRepository.seedTrackedTv(
                TrackedTvPollCandidate(
                    id = TvEpisodeNotificationPollerTestConstant.TV_ID,
                    lastKnownNextEpisodeAirDate = null,
                    lastKnownStatus = null,
                ),
            )
            val tvRepository = FakeTvRepository()
            tvRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(
                        id = TvEpisodeNotificationPollerTestConstant.TV_ID.toLong(),
                        title = TvEpisodeNotificationPollerTestConstant.SHOW_TITLE,
                        status = "Returning Series",
                        nextEpisodeToAir = Episode(airDate = "2026-09-10"),
                    ),
                )
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(trackedMediaRepository, tvRepository, notificationLedgerRepository)

            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
            assertEquals(
                TvEpisodeNotificationPollerTestConstant.ANNOUNCED_REASON,
                notificationLedgerRepository.recordNotifiedCalls.single().reason,
            )
        }

    @Test
    fun testRepollingSameAirDateDoesNotReNotify() =
        runTest {
            val trackedMediaRepository = FakeTrackedMediaRepository()
            trackedMediaRepository.seedTrackedTv(
                TrackedTvPollCandidate(
                    id = TvEpisodeNotificationPollerTestConstant.TV_ID,
                    lastKnownNextEpisodeAirDate = null,
                    lastKnownStatus = null,
                ),
            )
            val tvRepository = FakeTvRepository()
            tvRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(
                        id = TvEpisodeNotificationPollerTestConstant.TV_ID.toLong(),
                        title = TvEpisodeNotificationPollerTestConstant.SHOW_TITLE,
                        status = "Returning Series",
                        nextEpisodeToAir = Episode(airDate = "2026-09-10"),
                    ),
                )
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(trackedMediaRepository, tvRepository, notificationLedgerRepository)

            poller.poll()
            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
        }

    @Test
    fun testAirDateEqualsTodayNotifiesUnderAiringReasonWithoutReTriggeringAnnounced() =
        runTest {
            val trackedMediaRepository = FakeTrackedMediaRepository()
            trackedMediaRepository.seedTrackedTv(
                TrackedTvPollCandidate(
                    id = TvEpisodeNotificationPollerTestConstant.TV_ID,
                    // Already-known air date, so "announced" was already recorded in an earlier poll.
                    lastKnownNextEpisodeAirDate = TvEpisodeNotificationPollerTestConstant.TODAY,
                    lastKnownStatus = null,
                ),
            )
            val tvRepository = FakeTvRepository()
            tvRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(
                        id = TvEpisodeNotificationPollerTestConstant.TV_ID.toLong(),
                        title = TvEpisodeNotificationPollerTestConstant.SHOW_TITLE,
                        status = "Returning Series",
                        nextEpisodeToAir =
                            Episode(
                                airDate = TvEpisodeNotificationPollerTestConstant.TODAY,
                            ),
                    ),
                )
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(trackedMediaRepository, tvRepository, notificationLedgerRepository)

            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
            assertEquals(
                TvEpisodeNotificationPollerTestConstant.AIRING_REASON,
                notificationLedgerRepository.recordNotifiedCalls.single().reason,
            )
        }

    @Test
    fun testNextSeasonAirDateNotifiesAgainUnderSameReason() =
        runTest {
            val trackedMediaRepository = FakeTrackedMediaRepository()
            trackedMediaRepository.seedTrackedTv(
                TrackedTvPollCandidate(
                    id = TvEpisodeNotificationPollerTestConstant.TV_ID,
                    lastKnownNextEpisodeAirDate = null,
                    lastKnownStatus = null,
                ),
            )
            val tvRepository = FakeTvRepository()
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(trackedMediaRepository, tvRepository, notificationLedgerRepository)

            tvRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(
                        id = TvEpisodeNotificationPollerTestConstant.TV_ID.toLong(),
                        title = TvEpisodeNotificationPollerTestConstant.SHOW_TITLE,
                        status = "Returning Series",
                        nextEpisodeToAir = Episode(airDate = "2026-09-10"),
                    ),
                )
            poller.poll()

            tvRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(
                        id = TvEpisodeNotificationPollerTestConstant.TV_ID.toLong(),
                        title = TvEpisodeNotificationPollerTestConstant.SHOW_TITLE,
                        status = "Returning Series",
                        nextEpisodeToAir = Episode(airDate = "2027-03-01"),
                    ),
                )
            poller.poll()

            assertEquals(2, notificationLedgerRepository.recordNotifiedCalls.size)
            assertTrue(
                notificationLedgerRepository.recordNotifiedCalls.all {
                    it.reason ==
                        TvEpisodeNotificationPollerTestConstant.ANNOUNCED_REASON
                },
            )
        }

    @Test
    fun testEndedShowIsSkippedWithNoNetworkCallOnNextPoll() =
        runTest {
            val trackedMediaRepository = FakeTrackedMediaRepository()
            trackedMediaRepository.seedTrackedTv(
                TrackedTvPollCandidate(
                    id = TvEpisodeNotificationPollerTestConstant.TV_ID,
                    lastKnownNextEpisodeAirDate = null,
                    lastKnownStatus = "Ended",
                ),
            )
            val tvRepository = FakeTvRepository()
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(trackedMediaRepository, tvRepository, notificationLedgerRepository)

            poller.poll()

            assertTrue(tvRepository.getTvDetailsCalls.isEmpty())
            assertTrue(notificationLedgerRepository.recordNotifiedCalls.isEmpty())
        }

    @Test
    fun testPerShowExceptionDoesNotStopPollingTheRestOfTheBatch() =
        runTest {
            val failingId = 1
            val healthyId = 2
            val trackedMediaRepository = FakeTrackedMediaRepository()
            trackedMediaRepository.seedTrackedTv(
                TrackedTvPollCandidate(id = failingId, lastKnownNextEpisodeAirDate = null, lastKnownStatus = null),
            )
            trackedMediaRepository.seedTrackedTv(
                TrackedTvPollCandidate(id = healthyId, lastKnownNextEpisodeAirDate = null, lastKnownStatus = null),
            )
            val tvRepository =
                object : TvRepository by FakeTvRepository() {
                    override suspend fun getTvDetails(tvId: Long): TvDetail {
                        if (tvId == failingId.toLong()) throw IOException("network down")
                        return TvDetail(
                            id = tvId,
                            title = TvEpisodeNotificationPollerTestConstant.SHOW_TITLE,
                            status = "Returning Series",
                            nextEpisodeToAir = Episode(airDate = "2026-09-10"),
                        )
                    }
                }
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(trackedMediaRepository, tvRepository, notificationLedgerRepository)

            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
            assertEquals(healthyId, notificationLedgerRepository.recordNotifiedCalls.single().id)
        }
}
