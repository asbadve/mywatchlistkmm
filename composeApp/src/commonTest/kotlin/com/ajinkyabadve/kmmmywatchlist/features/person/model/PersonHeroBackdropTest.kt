package com.ajinkyabadve.kmmmywatchlist.features.person.model

import com.ajinkyabadve.kmmmywatchlist.features.person.PersonTestConstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersonHeroBackdropTest {
    private fun credit(
        id: Long,
        title: String,
        voteCount: Int = 0,
        backdropPath: String? = null,
        department: String? = null,
        mediaType: String = PersonTestConstant.MEDIA_TYPE_MOVIE,
        releaseDate: String? = null,
        firstAirDate: String? = null,
    ) = PersonCredit(
        id = id,
        mediaType = mediaType,
        title = title,
        voteCount = voteCount,
        backdropPath = backdropPath,
        department = department,
        releaseDate = releaseDate,
        firstAirDate = firstAirDate,
    )

    private fun person(
        knownForDepartment: String? = DEPARTMENT_ACTING,
        cast: List<PersonCredit> = emptyList(),
        crew: List<PersonCredit> = emptyList(),
    ) = PersonDetail(
        id = 1,
        name = "Someone",
        knownForDepartment = knownForDepartment,
        combinedCredits = PersonCombinedCredits(cast = cast, crew = crew),
    )

    /**
     * The whole point of borrowing from [PersonDetail.knownForCredits] rather than sorting by
     * popularity: popularity is current buzz, so a chat-show appearance from last week outranks the
     * film the person is actually known for. Vote count does not have that problem.
     */
    @Test
    fun testPicksTheMostVotedCreditNotTheMostRecent() {
        val subject =
            person(
                cast =
                    listOf(
                        credit(id = 1, title = "Last Week's Talk Show", voteCount = 40, backdropPath = "/talkshow.jpg"),
                        credit(id = 2, title = "The Career-Defining Film", voteCount = 30_000, backdropPath = "/iconic.jpg"),
                    ),
            )

        assertEquals("/iconic.jpg", subject.heroBackdropCredit()?.backdropPath)
    }

    /** A higher-ranked credit without artwork must not veto the banner entirely. */
    @Test
    fun testSkipsTopCreditsThatHaveNoBackdrop() {
        val subject =
            person(
                cast =
                    listOf(
                        credit(id = 1, title = "Bigger But Artless", voteCount = 50_000),
                        credit(id = 2, title = "Smaller With Artwork", voteCount = 100, backdropPath = "/art.jpg"),
                    ),
            )

        assertEquals("/art.jpg", subject.heroBackdropCredit()?.backdropPath)
    }

    /** Directors and other crew are ranked within their own department, as knownForCredits does. */
    @Test
    fun testUsesTheDepartmentCreditsForCrewMembers() {
        val subject =
            person(
                knownForDepartment = DEPARTMENT_DIRECTING,
                cast = listOf(credit(id = 1, title = "A Bit Part", voteCount = 90_000, backdropPath = "/cameo.jpg")),
                crew =
                    listOf(
                        credit(
                            id = 2,
                            title = "The Film They Directed",
                            voteCount = 40_000,
                            backdropPath = "/directed.jpg",
                            department = DEPARTMENT_DIRECTING,
                        ),
                    ),
            )

        assertEquals("/directed.jpg", subject.heroBackdropCredit()?.backdropPath)
    }

    /** Newcomers and most crew have nothing to show - callers fall back to the plain header. */
    @Test
    fun testReturnsNullWhenNoCreditHasABackdrop() {
        val subject = person(cast = listOf(credit(id = 1, title = "Artless", voteCount = 10)))

        assertNull(subject.heroBackdropCredit())
    }

    @Test
    fun testReturnsNullWhenThereAreNoCreditsAtAll() {
        assertNull(PersonDetail(id = 1, name = "Newcomer").heroBackdropCredit())
    }

    /** An empty string is what TMDB sometimes sends instead of omitting the field. */
    @Test
    fun testTreatsBlankBackdropPathAsAbsent() {
        val subject =
            person(
                cast =
                    listOf(
                        credit(id = 1, title = "Blank Path", voteCount = 900, backdropPath = ""),
                        credit(id = 2, title = "Real Path", voteCount = 100, backdropPath = "/real.jpg"),
                    ),
            )

        assertEquals("/real.jpg", subject.heroBackdropCredit()?.backdropPath)
    }

    /**
     * Regression test for a wrong number seen on device: Tom Holland's strip read "1988", eight
     * years before he was born. A TV credit carries the *series* first_air_date, so one guest slot
     * on a long-running show back-dates the person by decades. Films only.
     */
    @Test
    fun testFirstFilmYearIgnoresTheAirDateOfLongRunningShows() {
        val subject =
            person(
                cast =
                    listOf(
                        credit(
                            id = 1,
                            title = "A Chat Show That Started In 1988",
                            mediaType = PersonTestConstant.MEDIA_TYPE_TV,
                            firstAirDate = "1988-01-01",
                        ),
                        credit(
                            id = 2,
                            title = "Their Actual First Film",
                            mediaType = PersonTestConstant.MEDIA_TYPE_MOVIE,
                            releaseDate = "2012-10-11",
                        ),
                    ),
            )

        assertEquals(2012, subject.firstFilmYear())
    }

    @Test
    fun testFirstFilmYearIsNullWhenThePersonHasOnlyTelevisionCredits() {
        val subject =
            person(
                cast =
                    listOf(
                        credit(
                            id = 1,
                            title = "Only Ever On TV",
                            mediaType = PersonTestConstant.MEDIA_TYPE_TV,
                            firstAirDate = "2001-01-01",
                        ),
                    ),
            )

        assertNull(subject.firstFilmYear())
    }

    private companion object {
        /** TMDB department names, which `knownForCredits` matches crew entries on. */
        const val DEPARTMENT_ACTING = "Acting"
        const val DEPARTMENT_DIRECTING = "Directing"
    }
}
