package com.ajinkyabadve.kmmmywatchlist.features.person.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies PersonDetail parses the full TMDB person-details payload - shaped after the official
 * OpenAPI definition (developer.themoviedb.org/reference/person-details.md) and a live response
 * for person 500 with append_to_response=combined_credits,external_ids,images,translations.
 */
class PersonDetailSerializationTest {
    // Same relevant settings as TmdbClient's Json configuration.
    private val json = Json { ignoreUnknownKeys = true }

    private val fullPersonJson = """
        {
          "adult": false,
          "also_known_as": ["Thomas Cruise Mapother IV", "توم كروز"],
          "biography": "An American actor and producer.",
          "birthday": "1962-07-03",
          "deathday": null,
          "gender": 2,
          "homepage": "https://www.tomcruise.com",
          "id": 500,
          "imdb_id": "nm0000129",
          "known_for_department": "Acting",
          "name": "Tom Cruise",
          "place_of_birth": "Syracuse, New York, USA",
          "popularity": 14.9,
          "profile_path": "/8qBylBsQf4llkGrWR3qAsOtOU8O.jpg",
          "combined_credits": {
            "cast": [
              {
                "adult": false,
                "backdrop_path": "/PrMb3oeEluauy0q9ZO5xL33A6C.jpg",
                "genre_ids": [878, 28, 53],
                "id": 180,
                "title": "Minority Report",
                "original_title": "Minority Report",
                "overview": "John Anderton is a top Precrime cop.",
                "popularity": 7.8,
                "poster_path": "/ccqpHq5tk5W4ymbSbuoy4uYOxFI.jpg",
                "release_date": "2002-06-20",
                "vote_average": 7.356,
                "vote_count": 9722,
                "character": "Chief John Anderton",
                "credit_id": "52fe4223c3a36847f8006f53",
                "order": 0,
                "media_type": "movie"
              },
              {
                "id": 456,
                "name": "Some Show",
                "original_name": "Some Show",
                "first_air_date": "1999-01-31",
                "character": "Himself",
                "episode_count": 3,
                "media_type": "tv"
              }
            ],
            "crew": [
              {
                "id": 616,
                "title": "The Last Samurai",
                "release_date": "2003-12-05",
                "credit_id": "52fe425ec3a36847f8018e1f",
                "department": "Production",
                "job": "Producer",
                "media_type": "movie"
              }
            ]
          },
          "external_ids": {
            "freebase_mid": "/m/07r1h",
            "freebase_id": "/en/tom_cruise",
            "imdb_id": "nm0000129",
            "tvrage_id": 35,
            "wikidata_id": "Q37079",
            "facebook_id": "officialtomcruise",
            "instagram_id": "tomcruise",
            "tiktok_id": null,
            "twitter_id": "tomcruise",
            "youtube_id": null
          },
          "images": {
            "profiles": [
              { "file_path": "/p17SLq4wabXwIYyjXF1Wf5cNnAm.jpg", "width": 1684, "height": 2528 }
            ]
          },
          "translations": {
            "translations": [
              {
                "iso_3166_1": "US",
                "iso_639_1": "en",
                "name": "English",
                "english_name": "English",
                "data": { "biography": "Localized biography.", "name": "Tom Cruise" }
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun parsesEveryBaseFieldFromTheOasSchema() {
        val person = json.decodeFromString<PersonDetail>(fullPersonJson)

        assertEquals(500L, person.id)
        assertEquals("Tom Cruise", person.name)
        assertEquals(false, person.adult)
        assertEquals(listOf("Thomas Cruise Mapother IV", "توم كروز"), person.alsoKnownAs)
        assertEquals("An American actor and producer.", person.biography)
        assertEquals("1962-07-03", person.birthday)
        assertNull(person.deathday)
        assertEquals(2, person.gender)
        assertEquals("Male", person.genderLabel)
        assertEquals("https://www.tomcruise.com", person.homepage)
        assertEquals("nm0000129", person.imdbId)
        assertEquals("Acting", person.knownForDepartment)
        assertEquals("Syracuse, New York, USA", person.placeOfBirth)
        assertEquals(14.9, person.popularity)
        assertEquals("/8qBylBsQf4llkGrWR3qAsOtOU8O.jpg", person.profilePath)
    }

    @Test
    fun parsesCombinedCreditsWithMovieAndTvShapes() {
        val credits = assertNotNull(json.decodeFromString<PersonDetail>(fullPersonJson).combinedCredits)

        val movie = credits.cast.first()
        assertTrue(movie.isMovie)
        assertEquals("Minority Report", movie.displayTitle)
        assertEquals("2002-06-20", movie.displayDate)
        assertEquals("Chief John Anderton", movie.character)
        assertEquals(listOf(878, 28, 53), movie.genreIds)
        assertEquals(9722, movie.voteCount)

        val tv = credits.cast[1]
        assertEquals("tv", tv.mediaType)
        assertEquals("Some Show", tv.displayTitle)
        assertEquals("1999-01-31", tv.displayDate)
        assertEquals(3, tv.episodeCount)

        val crew = credits.crew.single()
        assertEquals("Producer", crew.job)
        assertEquals("Production", crew.department)
    }

    @Test
    fun parsesExternalIdsImagesAndTranslations() {
        val person = json.decodeFromString<PersonDetail>(fullPersonJson)

        val ids = assertNotNull(person.externalIds)
        assertEquals("nm0000129", ids.imdbId)
        assertEquals("tomcruise", ids.instagramId)
        assertEquals("tomcruise", ids.twitterId)
        assertEquals("officialtomcruise", ids.facebookId)
        assertEquals("Q37079", ids.wikidataId)
        assertEquals(35L, ids.tvrageId)
        assertNull(ids.tiktokId)
        assertNull(ids.youtubeId)

        assertEquals("/p17SLq4wabXwIYyjXF1Wf5cNnAm.jpg", assertNotNull(person.images).profiles.single().filePath)

        val translation = assertNotNull(person.translations).translations.single()
        assertEquals("US", translation.iso3166)
        assertEquals("Localized biography.", translation.data.biography)
        assertEquals("Tom Cruise", translation.data.name)
    }

    @Test
    fun knownCreditsCountCombinesCastAndCrew() {
        val person = json.decodeFromString<PersonDetail>(fullPersonJson)
        assertEquals(3, person.knownCreditsCount)
        assertEquals(0, PersonDetail().knownCreditsCount)
    }

    @Test
    fun filmographySectionsGroupActingFirstThenDepartmentsNewestFirst() {
        val credits = PersonCombinedCredits(
            cast = listOf(
                PersonCredit(id = 1, title = "Old Movie", releaseDate = "1999-01-01", mediaType = "movie"),
                PersonCredit(id = 2, title = "New Movie", releaseDate = "2024-05-01", mediaType = "movie"),
                PersonCredit(id = 3, title = "Upcoming", releaseDate = null, mediaType = "movie"),
            ),
            crew = listOf(
                PersonCredit(id = 4, title = "Produced Thing", department = "Production", job = "Producer"),
                PersonCredit(id = 5, title = "Directed Thing", department = "Directing", job = "Director"),
            ),
        )

        val sections = credits.filmographySections()

        assertEquals(listOf("Acting", "Directing", "Production"), sections.map { it.first })
        // Undated (upcoming) entries first, then newest to oldest.
        assertEquals(listOf("Upcoming", "New Movie", "Old Movie"), sections.first().second.map { it.displayTitle })
        assertTrue(PersonCombinedCredits().filmographySections().isEmpty())
    }

    @Test
    fun knownForUsesTheKnownForDepartmentRankedByVotes() {
        // Christopher Nolan case: cast credits are talk-show cameos, the real "known for" titles
        // are his Directing crew credits ranked by vote count.
        val director = PersonDetail(
            knownForDepartment = "Directing",
            combinedCredits = PersonCombinedCredits(
                cast = listOf(
                    PersonCredit(id = 1, name = "The Late Show", mediaType = "tv", voteCount = 100, character = "Self"),
                ),
                crew = listOf(
                    PersonCredit(id = 2, title = "Inception", mediaType = "movie", department = "Directing", job = "Director", voteCount = 39576),
                    PersonCredit(id = 3, title = "Interstellar", mediaType = "movie", department = "Directing", job = "Director", voteCount = 40333),
                    PersonCredit(id = 4, title = "Oppenheimer", mediaType = "movie", department = "Production", job = "Producer", voteCount = 9000),
                ),
            ),
        )
        assertEquals(listOf("Interstellar", "Inception"), director.knownForCredits().map { it.displayTitle })

        // Actors keep their cast credits.
        val actor = director.copy(knownForDepartment = "Acting")
        assertEquals(listOf("The Late Show"), actor.knownForCredits().map { it.displayTitle })

        // A declared department with no credits falls back to everything rather than showing nothing.
        val emptyDepartment = director.copy(knownForDepartment = "Sound")
        assertEquals(4, emptyDepartment.knownForCredits().size)

        assertTrue(PersonDetail().knownForCredits().isEmpty())
    }

    @Test
    fun filmographySectionsFilterByMediaType() {
        val credits = PersonCombinedCredits(
            cast = listOf(
                PersonCredit(id = 1, title = "A Movie", mediaType = "movie", releaseDate = "2020-01-01"),
                PersonCredit(id = 2, name = "A Show", mediaType = "tv", firstAirDate = "2021-01-01"),
            ),
            crew = listOf(
                PersonCredit(id = 3, title = "Directed Movie", mediaType = "movie", department = "Directing"),
            ),
        )

        val movieSections = credits.filmographySections(mediaType = "movie")
        assertEquals(listOf("Acting", "Directing"), movieSections.map { it.first })
        assertEquals(listOf("A Movie"), movieSections.first().second.map { it.displayTitle })

        val tvSections = credits.filmographySections(mediaType = "tv")
        assertEquals(listOf("Acting"), tvSections.map { it.first })
        assertEquals(listOf("A Show"), tvSections.first().second.map { it.displayTitle })
    }

    @Test
    fun yearsBetweenComputesWholeYearsAndHandlesBadInput() {
        // Birthday already passed in the target year.
        assertEquals(70, yearsBetween("1956-07-09", "2026-07-19"))
        // Birthday not reached yet in the target year.
        assertEquals(63, yearsBetween("1962-07-30", "2026-07-19"))
        assertNull(yearsBetween(null, "2026-07-19"))
        assertNull(yearsBetween("", "2026-07-19"))
        assertNull(yearsBetween("not-a-date", "2026-07-19"))
        assertNull(yearsBetween("1962-07-30", null))
    }

    @Test
    fun missingOptionalFieldsFallBackToDefaults() {
        val person = json.decodeFromString<PersonDetail>("""{"id": 1, "name": "Bare"}""")

        assertTrue(person.alsoKnownAs.isEmpty())
        assertEquals("", person.biography)
        assertNull(person.birthday)
        assertNull(person.genderLabel)
        assertNull(person.combinedCredits)
        assertNull(person.externalIds)
        assertNull(person.images)
        assertNull(person.translations)
    }
}
