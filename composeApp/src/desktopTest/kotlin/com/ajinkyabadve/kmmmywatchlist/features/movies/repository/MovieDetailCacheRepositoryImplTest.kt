package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.core.data.Resource
import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.network.HttpExceptionsTestFactory
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_movie_details
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MovieDetailCacheRepositoryImplTest {
    private suspend fun repository(fakeMovieRepository: FakeMovieRepository): MovieDetailCacheRepository {
        val database = createTestDatabase()
        return MovieDetailCacheRepositoryImpl(movieRepository = fakeMovieRepository, databaseProvider = { database })
    }

    // NetworkBoundResource's success path keeps emitting indefinitely (reactive to future local
    // writes), so `.toList()` would hang forever - the first non-Loading emission is this test's
    // "final" state, same as MovieDetailScreenModel only ever acting on Success/Error.
    private suspend fun <T> Flow<Resource<T>>.settled(): Resource<T> = first { it !is Resource.Loading }

    @Test
    fun testGetMovieDetailFetchesAndCachesTheDetail() =
        runTest {
            val detail = MovieDetail(id = 42, title = "Fixture Movie", overview = "A fixture overview.")
            val fakeMovieRepository = FakeMovieRepository().apply { getMovieDetailsResult = Result.success(detail) }
            val repository = repository(fakeMovieRepository)

            val resource = repository.getMovieDetail(42).settled()

            assertEquals(detail, (resource as Resource.Success).data)
            assertEquals(detail, repository.observe(42).first())
        }

    @Test
    fun testObserveEmitsNullOnACacheMiss() =
        runTest {
            assertNull(repository(FakeMovieRepository()).observe(42).first())
        }

    @Test
    fun testRefetchingOverwritesTheCachedRow() =
        runTest {
            val fakeMovieRepository =
                FakeMovieRepository().apply { getMovieDetailsResult = Result.success(MovieDetail(id = 42, title = "Old Title")) }
            val repository = repository(fakeMovieRepository)
            repository.getMovieDetail(42).settled()

            fakeMovieRepository.getMovieDetailsResult = Result.success(MovieDetail(id = 42, title = "New Title"))
            repository.getMovieDetail(42).settled()

            assertEquals("New Title", repository.observe(42).first()?.title)
        }

    @Test
    fun testHttpExceptionsMapsToThePlainResponseMessage() =
        runTest {
            val notFound = HttpExceptionsTestFactory.create(HttpStatusCode.NotFound)
            val fakeMovieRepository = FakeMovieRepository().apply { getMovieDetailsResult = Result.failure(notFound) }
            val repository = repository(fakeMovieRepository)

            val error = assertIs<Resource.Error<MovieDetail>>(repository.getMovieDetail(42).settled())

            assertEquals(UiText.Plain(notFound.message), error.message)
            assertNull(error.data)
        }

    @Test
    fun testIOExceptionMapsToTheNetworkErrorMessage() =
        runTest {
            val fakeMovieRepository =
                FakeMovieRepository().apply { getMovieDetailsResult = Result.failure(IOException("Mock network failure")) }
            val repository = repository(fakeMovieRepository)

            val error = assertIs<Resource.Error<MovieDetail>>(repository.getMovieDetail(42).settled())

            assertEquals(UiText.Resource(Res.string.error_network), error.message)
        }

    @Test
    fun testSerializationExceptionMapsToTheMalformedResponseMessage() =
        runTest {
            val fakeMovieRepository = FakeMovieRepository().apply { getMovieDetailsResult = Result.failure(SerializationException("Boom")) }
            val repository = repository(fakeMovieRepository)

            val error = assertIs<Resource.Error<MovieDetail>>(repository.getMovieDetail(42).settled())

            assertEquals(UiText.Resource(Res.string.error_unexpected_movie_details), error.message)
        }

    /** A failed fetch must still carry whatever was already cached, so a caller (the ScreenModel)
     *  can choose to keep showing it instead of surfacing the error. */
    @Test
    fun testAFailedFetchStillCarriesTheAlreadyCachedDetail() =
        runTest {
            val fakeMovieRepository =
                FakeMovieRepository().apply { getMovieDetailsResult = Result.success(MovieDetail(id = 42, title = "Cached")) }
            val repository = repository(fakeMovieRepository)
            repository.getMovieDetail(42).settled()

            fakeMovieRepository.getMovieDetailsResult = Result.failure(IOException("Mock network failure"))
            val error = assertIs<Resource.Error<MovieDetail>>(repository.getMovieDetail(42).settled())

            assertEquals("Cached", error.data?.title)
        }
}
