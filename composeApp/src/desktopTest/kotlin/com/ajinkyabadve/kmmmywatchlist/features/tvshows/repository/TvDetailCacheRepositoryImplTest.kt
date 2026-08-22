package com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository

import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.SeasonSummary
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.FakeTvRepository
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TvDetailCacheRepositoryImplTest {
    private suspend fun repository(fakeTvRepository: FakeTvRepository): TvDetailCacheRepository {
        val database = createTestDatabase()
        return TvDetailCacheRepositoryImpl(tvRepository = fakeTvRepository, databaseProvider = { database })
    }

    @Test
    fun testRefreshThenObserveEmitsTheFetchedDetailAndSeasons() =
        runTest {
            val fakeTvRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult =
                        Result.success(TvDetail(id = 1, title = "Fixture Show", seasons = listOf(SeasonSummary(seasonNumber = 1))))
                    getSeasonDetailsResultsByNumber[1] = Result.success(TvSeasonDetail(seasonNumber = 1, name = "Season 1"))
                }
            val repository = repository(fakeTvRepository)

            repository.refresh(1)

            assertEquals("Fixture Show", repository.observe(1).first()?.title)
            assertEquals(mapOf(1 to TvSeasonDetail(seasonNumber = 1, name = "Season 1")), repository.observeSeasons(1).first())
        }

    @Test
    fun testObserveEmitsNullOnACacheMiss() =
        runTest {
            assertNull(repository(FakeTvRepository()).observe(1).first())
        }

    @Test
    fun testObserveSeasonsIsEmptyWhenASeasonHasNeverBeenSuccessfullyFetched() =
        runTest {
            val fakeTvRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(TvDetail(id = 1, title = "Show", seasons = listOf(SeasonSummary(seasonNumber = 1))))
                    getSeasonDetailsResultsByNumber[1] = Result.failure(IOException("season unavailable"))
                }
            val repository = repository(fakeTvRepository)

            repository.refresh(1)

            assertEquals(emptyMap(), repository.observeSeasons(1).first())
        }

    /** A later refresh whose season fetch fails must not clobber a season already cached by an
     *  earlier successful refresh - this is the "don't let a bad fetch overwrite good cached data"
     *  rule, enforced by refresh() only ever upserting on success. */
    @Test
    fun testASeasonFetchFailureLeavesAPreviouslyCachedSeasonInPlace() =
        runTest {
            val fakeTvRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(TvDetail(id = 1, title = "Show", seasons = listOf(SeasonSummary(seasonNumber = 1))))
                    getSeasonDetailsResultsByNumber[1] = Result.success(TvSeasonDetail(seasonNumber = 1, name = "Season 1"))
                }
            val repository = repository(fakeTvRepository)
            repository.refresh(1)

            fakeTvRepository.getSeasonDetailsResultsByNumber[1] = Result.failure(IOException("season unavailable"))
            repository.refresh(1)

            assertEquals(mapOf(1 to TvSeasonDetail(seasonNumber = 1, name = "Season 1")), repository.observeSeasons(1).first())
        }
}
