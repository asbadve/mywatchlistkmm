package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/** Fixture values shared by the release-reminder tests in this package. */
object ReleaseReminderTestConstant {
    const val MOVIE_ID = 101L
    const val SHOW_ID = 202L
    const val SEASON_NUMBER = 1
    const val EPISODE_NUMBER = 8
    const val MOVIE_TITLE = "Upcoming Movie"
    const val SHOW_TITLE = "Lanterns"
    const val REGION_IN = "IN"
    const val REGION_US = "US"
    const val REGION_GB = "GB"

    val REMINDER_TIME = LocalTime(9, 0)
    val LATER_REMINDER_TIME = LocalTime(18, 30)

    /** "Now" for every scheduling test: the morning of 2026-10-03. */
    val NOW = LocalDateTime(2026, 10, 3, 8, 0)
    val TODAY: LocalDate = NOW.date
    val RELEASE_IN_FIVE_DAYS = LocalDate(2026, 10, 8)
    val RELEASE_TOMORROW = LocalDate(2026, 10, 4)
    val RELEASE_SLIPPED = LocalDate(2026, 10, 15)

    val MOVIE_KEY = ReminderKey(MOVIE_ID, MediaTypeConstant.MOVIE)
    val EPISODE_KEY = ReminderKey(SHOW_ID, MediaTypeConstant.TV, SEASON_NUMBER, EPISODE_NUMBER)

    fun movieReminder(releaseDate: LocalDate = RELEASE_IN_FIVE_DAYS): ReleaseReminder =
        ReleaseReminder(
            key = MOVIE_KEY,
            title = MOVIE_TITLE,
            posterPath = null,
            releaseDate = releaseDate,
            releaseSource = ReleaseSource(ReleaseSourceKind.PRIMARY).encode(),
        )

    fun episodeReminder(releaseDate: LocalDate = RELEASE_IN_FIVE_DAYS): ReleaseReminder =
        ReleaseReminder(
            key = EPISODE_KEY,
            title = SHOW_TITLE,
            posterPath = null,
            releaseDate = releaseDate,
            releaseSource = ReleaseSource(ReleaseSourceKind.EPISODE_AIR).encode(),
        )
}
