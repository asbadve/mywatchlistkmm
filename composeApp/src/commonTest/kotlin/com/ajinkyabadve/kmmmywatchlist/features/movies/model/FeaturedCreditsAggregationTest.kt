package com.ajinkyabadve.kmmmywatchlist.features.movies.model

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.CrewMember
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Dark Knight Collection scenario: featured cast/crew are aggregated across the member
 * movies' credits, mirroring what themoviedb.org collection pages display.
 */
class FeaturedCreditsAggregationTest {
    private val batmanBegins =
        Credits(
            cast =
                listOf(
                    CastMember(id = 3894, name = "Christian Bale", character = "Bruce Wayne", order = 0),
                    CastMember(id = 3895, name = "Michael Caine", character = "Alfred", order = 1),
                    CastMember(id = 6383, name = "Katie Holmes", character = "Rachel Dawes", order = 2),
                ),
            crew =
                listOf(
                    CrewMember(id = 525, name = "Christopher Nolan", job = "Director", department = "Directing"),
                    CrewMember(id = 282, name = "Emma Thomas", job = "Producer", department = "Production"),
                    CrewMember(id = 546, name = "David S. Goyer", job = "Screenplay", department = "Writing"),
                ),
        )

    private val darkKnight =
        Credits(
            cast =
                listOf(
                    CastMember(id = 3894, name = "Christian Bale", character = "Batman", order = 0),
                    CastMember(id = 3895, name = "Michael Caine", character = "Alfred", order = 2),
                    CastMember(id = 5, name = "Heath Ledger", character = "Joker", order = 1),
                ),
            crew =
                listOf(
                    CrewMember(id = 525, name = "Christopher Nolan", job = "Director", department = "Directing"),
                    CrewMember(id = 525, name = "Christopher Nolan", job = "Writer", department = "Writing"),
                    CrewMember(id = 282, name = "Emma Thomas", job = "Producer", department = "Production"),
                ),
        )

    @Test
    fun castRankedByAppearancesThenBilling_withMergedCharacters() {
        val featured = aggregateFeaturedCast(listOf(batmanBegins, darkKnight))

        // Bale and Caine appear in both films; Bale's billing (0) beats Caine's (1).
        assertEquals(listOf("Christian Bale", "Michael Caine"), featured.take(2).map { it.name })
        assertEquals("Bruce Wayne / Batman", featured.first().character)
        // Single-appearance actors follow, ranked by their best billing.
        assertEquals(listOf("Heath Ledger", "Katie Holmes"), featured.drop(2).map { it.name })
    }

    @Test
    fun crewPutsDirectorsFirstThenWriters_withMergedJobs() {
        val featured = aggregateFeaturedCrew(listOf(batmanBegins, darkKnight))

        assertEquals("Christopher Nolan", featured.first().name)
        assertEquals("Director, Writer", featured.first().character)
        // Goyer (Writing) outranks Thomas (Production) despite fewer appearances.
        assertEquals(listOf("David S. Goyer", "Emma Thomas"), featured.drop(1).map { it.name })
    }

    @Test
    fun maxLimitsAndEmptyInputAreRespected() {
        assertEquals(1, aggregateFeaturedCast(listOf(batmanBegins, darkKnight), max = 1).size)
        assertTrue(aggregateFeaturedCast(emptyList()).isEmpty())
        assertTrue(aggregateFeaturedCrew(emptyList()).isEmpty())
    }
}
