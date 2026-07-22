package com.ajinkyabadve.kmmmywatchlist.core

import com.ajinkyabadve.kmmmywatchlist.core.model.ImagesConfig
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.ConfigurationRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImageConfigResolverTest {
    private class FakeConfigurationRepository(
        private val imagesConfig: ImagesConfig,
    ) : ConfigurationRepository {
        override suspend fun getConfiguration(): ImagesConfig = imagesConfig
    }

    private val testConfig =
        ImagesConfig(
            base_url = "http://image.tmdb.org/t/p/",
            secure_base_url = "https://image.tmdb.org/t/p/",
            backdrop_sizes = listOf("w300", "w780", "w1280", "original"),
            logo_sizes = listOf("w45", "w92", "w154", "w185", "w300", "w500", "original"),
            poster_sizes = listOf("w92", "w154", "w185", "w342", "w500", "w780", "original"),
            profile_sizes = listOf("w45", "w185", "h632", "original"),
            still_sizes = listOf("w92", "w185", "w300", "original"),
        )

    @BeforeTest
    fun setUp() {
        // Inject fake repository configuration mapping into the resolver
        ImageConfigResolver.initialize(FakeConfigurationRepository(testConfig))
    }

    @Test
    fun testResolveReturnsNullForEmptyPath() {
        assertNull(ImageConfigResolver.resolve(null, ImageConfigResolver.ImageType.POSTER, 100, 1.0f))
        assertNull(ImageConfigResolver.resolve("", ImageConfigResolver.ImageType.POSTER, 100, 1.0f))
    }

    @Test
    fun testResolveMatchesBestPosterSizeForMdpi() {
        // 100dp width at 1.0f density = 100px. Smallest size >= 100px is "w154"
        val resolvedUrl = ImageConfigResolver.resolve("movie.jpg", ImageConfigResolver.ImageType.POSTER, 100, 1.0f)
        assertEquals("https://image.tmdb.org/t/p/w154/movie.jpg", resolvedUrl)
    }

    @Test
    fun testResolveMatchesBestPosterSizeForXxhdpi() {
        // 150dp width at 3.0f density = 450px. Smallest size >= 450px is "w500"
        val resolvedUrl = ImageConfigResolver.resolve("/movie.jpg", ImageConfigResolver.ImageType.POSTER, 150, 3.0f)
        assertEquals("https://image.tmdb.org/t/p/w500/movie.jpg", resolvedUrl)
    }

    @Test
    fun testResolveBackdropFallbacksToOriginal() {
        // Large width check 2000dp width at 2.0f density = 4000px. Fallback to "original"
        val resolvedUrl = ImageConfigResolver.resolve("backdrop.jpg", ImageConfigResolver.ImageType.BACKDROP, 2000, 2.0f)
        assertEquals("https://image.tmdb.org/t/p/original/backdrop.jpg", resolvedUrl)
    }
}
