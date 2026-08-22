package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MovieDetailCacheRepositoryImplTest {
    private suspend fun repository(fakeMovieRepository: FakeMovieRepository): MovieDetailCacheRepository {
        val database = createTestDatabase()
        return MovieDetailCacheRepositoryImpl(movieRepository = fakeMovieRepository, databaseProvider = { database })
    }

    @Test
    fun testRefreshThenObserveEmitsTheFetchedDetail() =
        runTest {
            val detail = MovieDetail(id = 42, title = "Fixture Movie", overview = "A fixture overview.")
            val fakeMovieRepository = FakeMovieRepository().apply { getMovieDetailsResult = Result.success(detail) }
            val repository = repository(fakeMovieRepository)

            repository.refresh(42)

            assertEquals(detail, repository.observe(42).first())
        }

    @Test
    fun testObserveEmitsNullOnACacheMiss() =
        runTest {
            assertNull(repository(FakeMovieRepository()).observe(42).first())
        }

    @Test
    fun testRefreshingTwiceOverwritesTheCachedRow() =
        runTest {
            val fakeMovieRepository = FakeMovieRepository()
            val repository = repository(fakeMovieRepository)
            fakeMovieRepository.getMovieDetailsResult = Result.success(MovieDetail(id = 42, title = "Old Title"))
            repository.refresh(42)

            fakeMovieRepository.getMovieDetailsResult = Result.success(MovieDetail(id = 42, title = "New Title"))
            repository.refresh(42)

            assertEquals("New Title", repository.observe(42).first()?.title)
        }
}
