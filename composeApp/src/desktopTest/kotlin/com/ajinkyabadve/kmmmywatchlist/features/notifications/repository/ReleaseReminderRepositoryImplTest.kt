package com.ajinkyabadve.kmmmywatchlist.features.notifications.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReleaseReminderRepositoryImplTest {
    private suspend fun repository(database: MyDatabase) =
        ReleaseReminderRepositoryImpl(databaseProvider = { database }, now = { NOW_MILLIS })

    @Test
    fun testSetReminder_insertsAndObservesThenDeletes() =
        runTest {
            val repository = repository(createTestDatabase())

            repository.setReminder(movieReminder(), enabled = true)
            assertTrue(repository.observeHasReminder(MOVIE_KEY).first())
            assertEquals(listOf(movieReminder()), repository.allReminders())

            repository.setReminder(movieReminder(), enabled = false)
            assertFalse(repository.observeHasReminder(MOVIE_KEY).first())
        }

    @Test
    fun testEpisodeKey_roundTripsThroughTheSentinelColumns() =
        runTest {
            val repository = repository(createTestDatabase())

            repository.setReminder(episodeReminder(), enabled = true)
            repository.setReminder(movieReminder(), enabled = true)

            assertEquals(setOf(EPISODE_KEY, MOVIE_KEY), repository.observeReminderKeys().first())
            assertEquals(EPISODE_KEY, repository.allReminders().first { it.key.isEpisode }.key)
        }

    @Test
    fun testSameTitleAddedTwice_isOneRow() =
        runTest {
            val repository = repository(createTestDatabase())

            repository.setReminder(movieReminder(), enabled = true)
            repository.setReminder(movieReminder(), enabled = true)

            assertEquals(1, repository.allReminders().size)
        }

    @Test
    fun testUpdateReleaseDate_changesDateAndSource() =
        runTest {
            val repository = repository(createTestDatabase())
            repository.setReminder(movieReminder(), enabled = true)

            repository.updateReleaseDate(MOVIE_KEY, SLIPPED_DATE, UPDATED_SOURCE)

            val stored = repository.allReminders().single()
            assertEquals(SLIPPED_DATE, stored.releaseDate)
            assertEquals(UPDATED_SOURCE, stored.releaseSource)
        }

    @Test
    fun testDeleteExpired_keepsRowsForTheGraceWindow() =
        runTest {
            val repository = repository(createTestDatabase())
            val withinGrace = TODAY.minus(DatePeriod(days = ReleaseReminderConstant.EXPIRY_DAYS))
            val pastGrace = TODAY.minus(DatePeriod(days = ReleaseReminderConstant.EXPIRY_DAYS + 1))
            repository.setReminder(movieReminder(releaseDate = withinGrace), enabled = true)
            repository.setReminder(episodeReminder(releaseDate = pastGrace), enabled = true)

            repository.deleteExpired(TODAY)

            assertEquals(listOf(MOVIE_KEY), repository.allReminders().map { it.key })
        }

    @Test
    fun testPreference_defaultsToNineAmAndRoundTrips() =
        runTest {
            val repository = repository(createTestDatabase())

            assertEquals(ReminderPreference(enabled = true, time = ReleaseReminderConstant.DEFAULT_TIME), repository.preference())

            val changed = ReminderPreference(enabled = false, time = CHANGED_TIME)
            repository.setPreference(changed)

            assertEquals(changed, repository.preference())
            assertEquals(changed, repository.observePreference().first())
        }

    @Test
    fun testDeleteAll_removesEveryReminder() =
        runTest {
            val repository = repository(createTestDatabase())
            repository.setReminder(movieReminder(), enabled = true)
            repository.setReminder(episodeReminder(), enabled = true)

            repository.deleteAll()

            assertTrue(repository.allReminders().isEmpty())
        }

    private fun movieReminder(releaseDate: LocalDate = RELEASE_DATE) =
        ReleaseReminder(key = MOVIE_KEY, title = MOVIE_TITLE, posterPath = POSTER_PATH, releaseDate = releaseDate, releaseSource = SOURCE)

    private fun episodeReminder(releaseDate: LocalDate = RELEASE_DATE) =
        ReleaseReminder(key = EPISODE_KEY, title = SHOW_TITLE, posterPath = null, releaseDate = releaseDate, releaseSource = null)

    private companion object {
        const val NOW_MILLIS = 1_000L
        const val MOVIE_TITLE = "Upcoming Movie"
        const val SHOW_TITLE = "Lanterns"
        const val POSTER_PATH = "/poster.jpg"
        const val SOURCE = "primary"
        const val UPDATED_SOURCE = "theatrical:IN"
        val MOVIE_KEY = ReminderKey(101L, MediaTypeConstant.MOVIE)
        val EPISODE_KEY = ReminderKey(202L, MediaTypeConstant.TV, seasonNumber = 1, episodeNumber = 8)
        val TODAY = LocalDate(2026, 10, 3)
        val RELEASE_DATE = LocalDate(2026, 10, 8)
        val SLIPPED_DATE = LocalDate(2026, 10, 15)
        val CHANGED_TIME = LocalTime(18, 30)
    }
}
