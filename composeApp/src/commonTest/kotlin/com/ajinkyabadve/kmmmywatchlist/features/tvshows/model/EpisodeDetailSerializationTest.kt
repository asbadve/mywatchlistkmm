package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies EpisodeDetail parses the full TMDB episode-details payload - shaped after the official
 * OpenAPI definition (developer.themoviedb.org/reference/tv-episode-details.md) and a live
 * response for GoT S01E01 with append_to_response=credits,external_ids,images,translations,videos.
 */
class EpisodeDetailSerializationTest {
    // Same relevant settings as TmdbClient's Json configuration.
    private val json = Json { ignoreUnknownKeys = true }

    private val fullEpisodeJson =
        """
        {
          "air_date": "2011-04-17",
          "episode_number": 1,
          "episode_type": "finale",
          "id": 63056,
          "name": "Winter Is Coming",
          "overview": "Jon Arryn, the Hand of the King, is dead.",
          "production_code": "101",
          "runtime": 62,
          "season_number": 1,
          "still_path": "/9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg",
          "vote_average": 7.8,
          "vote_count": 457,
          "crew": [
            {
              "department": "Directing",
              "job": "Director",
              "credit_id": "5256c8a219c2956ff6046e77",
              "adult": false,
              "gender": 2,
              "id": 44797,
              "known_for_department": "Directing",
              "name": "Timothy Van Patten",
              "original_name": "Timothy Van Patten",
              "popularity": 7.775,
              "profile_path": "/MzSOFrd99HRdr6pkSRSctk3kBR.jpg"
            },
            {
              "department": "Writing",
              "job": "Writer",
              "credit_id": "5256c8a019c2956ff6046e2b",
              "id": 9813,
              "name": "David Benioff",
              "original_name": "David Benioff",
              "profile_path": null
            }
          ],
          "guest_stars": [
            {
              "character": "Khal Drogo",
              "credit_id": "5256c8a219c2956ff6046f40",
              "order": 63,
              "id": 117642,
              "name": "Jason Momoa",
              "profile_path": "/6dEFBpZH8C8OijsynkSajQT99Pb.jpg"
            }
          ],
          "credits": {
            "cast": [
              {
                "id": 22970,
                "name": "Peter Dinklage",
                "character": "Tyrion Lannister",
                "profile_path": "/9CAd7wr8QZyIN0E7nm8v1B6WkGn.jpg"
              }
            ],
            "crew": [
              {
                "department": "Directing",
                "job": "Director",
                "id": 44797,
                "name": "Timothy Van Patten"
              }
            ],
            "guest_stars": [
              {
                "id": 117642,
                "name": "Jason Momoa",
                "character": "Khal Drogo"
              }
            ]
          },
          "external_ids": {
            "imdb_id": "tt1480055",
            "freebase_mid": "/m/0gmc6ph",
            "freebase_id": "/en/winter_is_coming",
            "tvdb_id": 3254641,
            "tvrage_id": 1065008299,
            "wikidata_id": "Q2614622"
          },
          "images": {
            "stills": [
              { "file_path": "/still1.jpg", "width": 1920, "height": 1080 }
            ]
          },
          "videos": {
            "results": [
              { "id": "abc", "key": "yt123", "name": "Trailer", "site": "YouTube", "type": "Trailer", "official": true }
            ]
          },
          "translations": {
            "translations": [
              {
                "iso_3166_1": "US",
                "iso_639_1": "en",
                "name": "English",
                "english_name": "English",
                "data": { "name": "Winter Is Coming", "overview": "Localized overview." }
              }
            ]
          }
        }
        """.trimIndent()

    @Test
    fun parsesEveryBaseFieldFromTheOasSchema() {
        val episode = json.decodeFromString<EpisodeDetail>(fullEpisodeJson)

        assertEquals(63056L, episode.id)
        assertEquals("Winter Is Coming", episode.name)
        assertEquals("Jon Arryn, the Hand of the King, is dead.", episode.overview)
        assertEquals("2011-04-17", episode.airDate)
        assertEquals(1, episode.episodeNumber)
        assertEquals(1, episode.seasonNumber)
        assertEquals("finale", episode.episodeType)
        assertEquals("101", episode.productionCode)
        assertEquals(62, episode.runtime)
        assertEquals("/9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg", episode.stillPath)
        assertEquals(7.8, episode.voteAverage)
        assertEquals(457, episode.voteCount)
    }

    @Test
    fun parsesTopLevelCrewWithJobAndDepartment() {
        val episode = json.decodeFromString<EpisodeDetail>(fullEpisodeJson)

        assertEquals(2, episode.crew.size)
        val director = episode.crew.first()
        assertEquals("Timothy Van Patten", director.name)
        assertEquals("Director", director.job)
        assertEquals("Directing", director.department)
        assertEquals("5256c8a219c2956ff6046e77", director.creditId)
        assertEquals("Directing", director.knownForDepartment)
        assertEquals("/MzSOFrd99HRdr6pkSRSctk3kBR.jpg", director.profilePath)

        val writer = episode.crew[1]
        assertEquals("Writer", writer.job)
        assertEquals("Writing", writer.department)
        assertEquals(null, writer.profilePath)
    }

    @Test
    fun parsesAppendedCreditsWithCastCrewAndGuestStars() {
        val episode = json.decodeFromString<EpisodeDetail>(fullEpisodeJson)

        val credits = assertNotNull(episode.credits)
        assertEquals("Peter Dinklage", credits.cast.single().name)
        assertEquals("Tyrion Lannister", credits.cast.single().character)
        assertEquals("Timothy Van Patten", credits.crew.single().name)
        assertEquals("Jason Momoa", credits.guestStars.single().name)
    }

    @Test
    fun parsesAllExternalIds() {
        val ids = assertNotNull(json.decodeFromString<EpisodeDetail>(fullEpisodeJson).externalIds)

        assertEquals("tt1480055", ids.imdbId)
        assertEquals(3254641L, ids.tvdbId)
        assertEquals("Q2614622", ids.wikidataId)
        assertEquals("/m/0gmc6ph", ids.freebaseMid)
        assertEquals("/en/winter_is_coming", ids.freebaseId)
        assertEquals(1065008299L, ids.tvrageId)
    }

    @Test
    fun parsesTranslationsImagesAndVideos() {
        val episode = json.decodeFromString<EpisodeDetail>(fullEpisodeJson)

        val translation = assertNotNull(episode.translations).translations.single()
        assertEquals("US", translation.iso3166)
        assertEquals("en", translation.iso639)
        assertEquals("English", translation.englishName)
        assertEquals("Winter Is Coming", translation.data.name)
        assertEquals("Localized overview.", translation.data.overview)

        assertEquals("/still1.jpg", assertNotNull(episode.images).stills.single().filePath)
        assertEquals("yt123", assertNotNull(episode.videos).results.single().key)
    }

    @Test
    fun allCrewPrefersTopLevelCrewAndFallsBackToCredits() {
        val episode = json.decodeFromString<EpisodeDetail>(fullEpisodeJson)
        assertEquals(2, episode.allCrew.size)

        val creditsOnly =
            EpisodeDetail(
                credits = EpisodeCredits(crew = listOf(CrewMember(name = "Only In Credits", job = "Director"))),
            )
        assertEquals("Only In Credits", creditsOnly.allCrew.single().name)

        assertTrue(EpisodeDetail().allCrew.isEmpty())
    }

    @Test
    fun missingOptionalFieldsFallBackToDefaults() {
        val episode = json.decodeFromString<EpisodeDetail>("""{"id": 1, "name": "Bare"}""")

        assertEquals(null, episode.episodeType)
        assertEquals(null, episode.productionCode)
        assertEquals(0, episode.voteCount)
        assertTrue(episode.crew.isEmpty())
        assertEquals(null, episode.translations)
        assertEquals(null, episode.externalIds)
    }
}
