package com.ajinkyabadve.kmmmywatchlist.features.movies.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies MovieDetail parses the full TMDB movie-details payload - shaped after a live response
 * for The Odyssey (1368337) with the repository's full append_to_response list including
 * watch/providers.
 */
class MovieDetailSerializationTest {
    // Same relevant settings as TmdbClient's Json configuration.
    private val json = Json { ignoreUnknownKeys = true }

    private val fullMovieJson =
        """
        {
          "id": 1368337,
          "title": "The Odyssey",
          "original_title": "The Odyssey",
          "overview": "After the fall of Troy, Odysseus embarks on a long journey home.",
          "tagline": "Defy the gods.",
          "status": "Released",
          "backdrop_path": "/backdrop.jpg",
          "poster_path": "/poster.jpg",
          "release_date": "2026-07-15",
          "vote_average": 7.2,
          "vote_count": 524,
          "popularity": 660.49,
          "runtime": 158,
          "budget": 250000000,
          "revenue": 72980296,
          "homepage": "https://www.universalpictures.com/movies/the-odyssey",
          "genres": [{ "id": 12, "name": "Adventure" }],
          "original_language": "en",
          "origin_country": ["GB", "US"],
          "spoken_languages": [
            { "english_name": "English", "iso_639_1": "en", "name": "English" }
          ],
          "production_companies": [
            { "id": 33, "logo_path": "/universal.png", "name": "Universal Pictures", "origin_country": "US" },
            { "id": 9996, "logo_path": null, "name": "Syncopy", "origin_country": "GB" }
          ],
          "production_countries": [
            { "iso_3166_1": "US", "name": "United States of America" }
          ],
          "belongs_to_collection": {
            "id": 1,
            "name": "Epic Collection",
            "poster_path": "/cp.jpg",
            "backdrop_path": "/cb.jpg"
          },
          "external_ids": {
            "imdb_id": "tt33764258",
            "wikidata_id": "Q131547207",
            "facebook_id": "OdysseyMovie",
            "instagram_id": "theodysseymovie",
            "twitter_id": "odysseymovie"
          },
          "credits": {
            "cast": [
              { "id": 2524, "name": "Tom Holland", "character": "Telemachus", "profile_path": "/th.jpg" }
            ],
            "crew": [
              { "id": 525, "name": "Christopher Nolan", "job": "Director", "department": "Directing" },
              { "id": 525, "name": "Christopher Nolan", "job": "Writer", "department": "Writing" }
            ]
          },
          "watch/providers": {
            "results": {
              "IN": {
                "link": "https://www.themoviedb.org/movie/1368337/watch?locale=IN",
                "flatrate": [
                  { "provider_id": 8, "provider_name": "Netflix", "logo_path": "/netflix.jpg", "display_priority": 2 }
                ],
                "rent": [
                  { "provider_id": 3, "provider_name": "Google Play Movies", "logo_path": "/gp.jpg", "display_priority": 6 }
                ]
              }
            }
          }
        }
        """.trimIndent()

    @Test
    fun parsesAllNewBaseFields() {
        val movie = json.decodeFromString<MovieDetail>(fullMovieJson)

        assertEquals("Defy the gods.", movie.tagline)
        assertEquals("Released", movie.status)
        assertEquals("The Odyssey", movie.originalTitle)
        assertEquals(524, movie.voteCount)
        assertEquals(660.49, movie.popularity)
        assertEquals(250000000L, movie.budget)
        assertEquals(72980296L, movie.revenue)
        assertEquals("https://www.universalpictures.com/movies/the-odyssey", movie.homepage)
        assertEquals(listOf("GB", "US"), movie.originCountry)
        assertEquals("English", movie.spokenLanguages.single().englishName)
        assertEquals(listOf("Universal Pictures", "Syncopy"), movie.productionCompanies.map { it.name })
        assertEquals("United States of America", movie.productionCountries.single().name)
    }

    @Test
    fun parsesCollectionExternalIdsAndCrew() {
        val movie = json.decodeFromString<MovieDetail>(fullMovieJson)

        val collection = assertNotNull(movie.belongsToCollection)
        assertEquals("Epic Collection", collection.name)
        assertEquals("/cb.jpg", collection.backdropPath)

        val ids = assertNotNull(movie.externalIds)
        assertEquals("tt33764258", ids.imdbId)
        assertEquals("Q131547207", ids.wikidataId)

        val credits = assertNotNull(movie.credits)
        assertEquals("Telemachus", credits.cast.single().character)
        assertEquals(listOf("Director", "Writer"), credits.crew.map { it.job })
    }

    @Test
    fun parsesWatchProvidersKeyedByRegion() {
        val movie = json.decodeFromString<MovieDetail>(fullMovieJson)

        val india = assertNotNull(movie.watchProviders).results.getValue("IN")
        assertTrue(india.link.contains("/watch"))
        assertEquals("Netflix", india.flatrate.single().providerName)
        assertEquals("Google Play Movies", india.rent.single().providerName)
        assertTrue(india.buy.isEmpty())
    }

    @Test
    fun missingOptionalFieldsFallBackToDefaults() {
        val movie = json.decodeFromString<MovieDetail>("""{"id": 1, "title": "Bare"}""")

        assertNull(movie.tagline)
        assertNull(movie.status)
        assertNull(movie.revenue)
        assertNull(movie.belongsToCollection)
        assertNull(movie.externalIds)
        assertNull(movie.watchProviders)
        assertEquals(0, movie.voteCount)
        assertTrue(movie.productionCompanies.isEmpty())
        assertTrue(movie.spokenLanguages.isEmpty())
    }
}
