package com.ajinkyabadve.kmmmywatchlist.features.movies.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies CollectionDetail parses the TMDB collection-details payload - shaped after a live
 * response for The Dark Knight Collection (263) with append_to_response=images,translations.
 */
class CollectionDetailSerializationTest {
    // Same relevant settings as TmdbClient's Json configuration.
    private val json = Json { ignoreUnknownKeys = true }

    private val fullCollectionJson =
        """
        {
          "id": 263,
          "name": "The Dark Knight Collection",
          "original_language": "en",
          "original_name": "The Dark Knight Collection",
          "overview": "Christopher Nolan's Batman trilogy.",
          "poster_path": "/poster.jpg",
          "backdrop_path": "/backdrop.jpg",
          "parts": [
            {
              "id": 272,
              "title": "Batman Begins",
              "overview": "Bruce Wayne becomes Batman.",
              "poster_path": "/bb.jpg",
              "release_date": "2005-06-10",
              "vote_average": 7.7,
              "media_type": "movie"
            },
            {
              "id": 155,
              "title": "The Dark Knight",
              "overview": "Batman faces the Joker.",
              "poster_path": "/tdk.jpg",
              "release_date": "2008-07-16",
              "vote_average": 8.5,
              "media_type": "movie"
            },
            {
              "id": 999999,
              "title": "Unreleased Part",
              "overview": "",
              "poster_path": null,
              "release_date": "",
              "vote_average": 0.0,
              "media_type": "movie"
            }
          ],
          "images": {
            "backdrops": [{ "file_path": "/b1.jpg", "width": 1920, "height": 1080 }],
            "posters": [{ "file_path": "/p1.jpg", "width": 500, "height": 750 }]
          },
          "translations": {
            "translations": [
              {
                "iso_3166_1": "US",
                "iso_639_1": "en",
                "name": "English",
                "english_name": "English",
                "data": { "title": "The Dark Knight Collection", "overview": "Localized overview." }
              }
            ]
          }
        }
        """.trimIndent()

    @Test
    fun parsesBasePartsImagesAndTranslations() {
        val collection = json.decodeFromString<CollectionDetail>(fullCollectionJson)

        assertEquals(263L, collection.id)
        assertEquals("The Dark Knight Collection", collection.name)
        assertEquals("Christopher Nolan's Batman trilogy.", collection.overview)
        assertEquals(3, collection.parts.size)
        assertEquals("Batman Begins", collection.parts.first().title)
        assertEquals("/b1.jpg", assertNotNull(collection.images).backdrops.single().filePath)
        assertEquals(
            "Localized overview.",
            assertNotNull(collection.translations)
                .translations
                .single()
                .data
                ?.overview,
        )
    }

    @Test
    fun averageVoteIgnoresUnratedParts() {
        val collection = json.decodeFromString<CollectionDetail>(fullCollectionJson)
        // (7.7 + 8.5) / 2 - the unrated part must not drag the average down.
        assertEquals(8.1, assertNotNull(collection.averageVote), absoluteTolerance = 0.001)

        assertNull(CollectionDetail().averageVote)
    }

    @Test
    fun partsInReleaseOrderPutUndatedEntriesLast() {
        val collection = json.decodeFromString<CollectionDetail>(fullCollectionJson)
        assertEquals(
            listOf("Batman Begins", "The Dark Knight", "Unreleased Part"),
            collection.partsInReleaseOrder.map { it.title },
        )
    }

    @Test
    fun missingOptionalFieldsFallBackToDefaults() {
        val collection = json.decodeFromString<CollectionDetail>("""{"id": 1, "name": "Bare"}""")

        assertTrue(collection.parts.isEmpty())
        assertNull(collection.images)
        assertNull(collection.translations)
        assertNull(collection.averageVote)
    }
}
