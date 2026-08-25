package com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.core.data.Resource
import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.SeasonSummary
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.FakeTvRepository
import com.ajinkyabadve.kmmmywatchlist.network.HttpExceptionsTestFactory
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_tv_details
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class TvDetailCacheRepositoryImplTest {
    private suspend fun repository(fakeTvRepository: FakeTvRepository): TvDetailCacheRepository {
        val database = createTestDatabase()
        return TvDetailCacheRepositoryImpl(tvRepository = fakeTvRepository, databaseProvider = { database })
    }

    // See MovieDetailCacheRepositoryImplTest's identical helper for why this isn't `.toList()`.
    private suspend fun <T> Flow<Resource<T>>.settled(): Resource<T> = first { it !is Resource.Loading }

    @Test
    fun testGetTvDetailFetchesAndCachesTheDetailAndSeasons() =
        runTest {
            val fakeTvRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult =
                        Result.success(TvDetail(id = 1, title = "Fixture Show", seasons = listOf(SeasonSummary(seasonNumber = 1))))
                    getSeasonDetailsResultsByNumber[1] = Result.success(TvSeasonDetail(seasonNumber = 1, name = "Season 1"))
                }
            val repository = repository(fakeTvRepository)

            val resource = repository.getTvDetail(1).settled()

            assertEquals("Fixture Show", (resource as Resource.Success).data.first.title)
            assertEquals(mapOf(1 to TvSeasonDetail(seasonNumber = 1, name = "Season 1")), resource.data.second)
            assertEquals("Fixture Show", repository.observe(1).first()?.title)
        }

    @Test
    fun testObserveEmitsNullOnACacheMiss() =
        runTest {
            assertNull(repository(FakeTvRepository()).observe(1).first())
        }

    @Test
    fun testASeasonThatFailsToFetchIsExcludedFromTheResult() =
        runTest {
            val fakeTvRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(TvDetail(id = 1, title = "Show", seasons = listOf(SeasonSummary(seasonNumber = 1))))
                    getSeasonDetailsResultsByNumber[1] = Result.failure(IOException("season unavailable"))
                }
            val repository = repository(fakeTvRepository)

            val resource = repository.getTvDetail(1).settled()

            assertEquals(emptyMap(), (resource as Resource.Success).data.second)
            assertEquals(emptyMap(), repository.observeSeasons(1).first())
        }

    /** A later fetch whose season fails must not clobber a season already cached by an earlier
     *  successful fetch - `saveCallResult` only ever upserts what's present in the result map. */
    @Test
    fun testASeasonFetchFailureLeavesAPreviouslyCachedSeasonInPlace() =
        runTest {
            val fakeTvRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(TvDetail(id = 1, title = "Show", seasons = listOf(SeasonSummary(seasonNumber = 1))))
                    getSeasonDetailsResultsByNumber[1] = Result.success(TvSeasonDetail(seasonNumber = 1, name = "Season 1"))
                }
            val repository = repository(fakeTvRepository)
            repository.getTvDetail(1).settled()

            fakeTvRepository.getSeasonDetailsResultsByNumber[1] = Result.failure(IOException("season unavailable"))
            repository.getTvDetail(1).settled()

            assertEquals(mapOf(1 to TvSeasonDetail(seasonNumber = 1, name = "Season 1")), repository.observeSeasons(1).first())
        }

    @Test
    fun testHttpExceptionsMapsToThePlainResponseMessage() =
        runTest {
            val notFound = HttpExceptionsTestFactory.create(HttpStatusCode.NotFound)
            val fakeTvRepository = FakeTvRepository().apply { getTvDetailsResult = Result.failure(notFound) }
            val repository = repository(fakeTvRepository)

            val error = assertIs<Resource.Error<Pair<TvDetail, Map<Int, TvSeasonDetail>>>>(repository.getTvDetail(1).settled())

            assertEquals(UiText.Plain(notFound.message), error.message)
            assertNull(error.data)
        }

    @Test
    fun testIOExceptionMapsToTheNetworkErrorMessage() =
        runTest {
            val fakeTvRepository = FakeTvRepository().apply { getTvDetailsResult = Result.failure(IOException("Mock network failure")) }
            val repository = repository(fakeTvRepository)

            val error = assertIs<Resource.Error<Pair<TvDetail, Map<Int, TvSeasonDetail>>>>(repository.getTvDetail(1).settled())

            assertEquals(UiText.Resource(Res.string.error_network), error.message)
        }

    @Test
    fun testSerializationExceptionMapsToTheMalformedResponseMessage() =
        runTest {
            val fakeTvRepository = FakeTvRepository().apply { getTvDetailsResult = Result.failure(SerializationException("Boom")) }
            val repository = repository(fakeTvRepository)

            val error = assertIs<Resource.Error<Pair<TvDetail, Map<Int, TvSeasonDetail>>>>(repository.getTvDetail(1).settled())

            assertEquals(UiText.Resource(Res.string.error_unexpected_tv_details), error.message)
        }
}
