package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDateItem
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResult
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseType
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RegionalReleaseDateChipUiTest {
    // Mirrors the live TMDB shape for "Other Mommy": the primary date is the earliest release
    // anywhere, a few days before the viewer's own theatrical date.
    private val detail =
        MovieDetail(
            releaseDate = PRIMARY_DATE,
            releaseDates =
                ReleaseDatesResponse(
                    results =
                        listOf(
                            ReleaseDatesResult(
                                iso3166 = REGION_US,
                                releaseDates =
                                    listOf(
                                        ReleaseDateItem(
                                            certification = CERTIFICATION,
                                            releaseDate = US_THEATRICAL,
                                            type = ReleaseType.THEATRICAL.tmdbValue,
                                        ),
                                        ReleaseDateItem(
                                            note = DIGITAL_NOTE,
                                            releaseDate = US_DIGITAL,
                                            type = ReleaseType.DIGITAL.tmdbValue,
                                        ),
                                    ),
                            ),
                        ),
                ),
        )

    @Test
    fun testChip_showsTheRegionalTheatricalDateNotThePrimaryOne() =
        runComposeUiTest {
            setContent { RegionalReleaseDateChip(detail = detail, regionCode = REGION_US, fallbackRegionCode = REGION_US) }

            onNodeWithText(CHIP_LABEL_SUFFIX, substring = true).assertExists()
            onNodeWithText(PRIMARY_DATE_FORMATTED, substring = true).assertDoesNotExist()
        }

    @Test
    fun testTappingTheChip_listsEveryReleaseInTheRegionAndTheWorldwideDate() =
        runComposeUiTest {
            setContent { RegionalReleaseDateChip(detail = detail, regionCode = REGION_US, fallbackRegionCode = REGION_US) }

            onNodeWithText(CHIP_LABEL_SUFFIX, substring = true).performClick()

            onNodeWithText(THEATRICAL_ROW).assertExists()
            onNodeWithText(DIGITAL_ROW).assertExists()
            onNodeWithText(WORLDWIDE_ROW).assertExists()
        }

    @Test
    fun testNoRegionalData_fallsBackToThePrimaryDate() =
        runComposeUiTest {
            setContent {
                RegionalReleaseDateChip(
                    detail = MovieDetail(releaseDate = PRIMARY_DATE),
                    regionCode = REGION_US,
                    fallbackRegionCode = REGION_US,
                )
            }

            onNodeWithText(PRIMARY_DATE_FORMATTED).assertExists()
        }

    private companion object {
        const val REGION_US = "US"
        const val PRIMARY_DATE = "2026-10-07"
        const val PRIMARY_DATE_FORMATTED = "Oct 7, 2026"
        const val US_THEATRICAL = "2026-10-09T00:00:00.000Z"
        const val US_DIGITAL = "2026-11-24T00:00:00.000Z"
        const val CERTIFICATION = "R"
        const val DIGITAL_NOTE = "VOD"
        const val CHIP_LABEL_SUFFIX = "Oct 9, 2026 · In theaters"
        const val THEATRICAL_ROW = "In theaters · Oct 9, 2026 · R"
        const val DIGITAL_ROW = "Digital · Nov 24, 2026 · VOD"
        const val WORLDWIDE_ROW = "First released worldwide on Oct 7, 2026"
    }
}
