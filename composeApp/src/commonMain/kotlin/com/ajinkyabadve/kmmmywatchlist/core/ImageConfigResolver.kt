package com.ajinkyabadve.kmmmywatchlist.core

import com.ajinkyabadve.kmmmywatchlist.core.constant.ConfigurationConstants
import com.ajinkyabadve.kmmmywatchlist.core.model.ImagesConfig
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.ConfigurationRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.ConfigurationRepositoryImpl
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ImageConfigResolver {

    private var activeConfig: ImagesConfig = ConfigurationConstants.defaultImagesConfig
    private var repository: ConfigurationRepository = ConfigurationRepositoryImpl()

    /**
     * Initializes the resolver with a custom repository instance.
     * Useful for injecting mock repositories during unit testing.
     */
    fun initialize(configRepository: ConfigurationRepository) {
        repository = configRepository
        refreshConfig()
    }

    fun refreshConfig() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                activeConfig = repository.getConfiguration()
            } catch (e: io.ktor.client.plugins.ResponseException) {
                Napier.e("Response exception when loading config", e, tag = "ImageConfigResolver")
            } catch (e: io.ktor.client.network.sockets.ConnectTimeoutException) {
                Napier.e("Connection timeout when loading config", e, tag = "ImageConfigResolver")
            } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
                Napier.e("Request timeout when loading config", e, tag = "ImageConfigResolver")
            } catch (e: io.ktor.utils.io.errors.IOException) {
                Napier.e("Network IO exception when loading config", e, tag = "ImageConfigResolver")
            } catch (e: kotlinx.serialization.SerializationException) {
                Napier.e("Serialization exception when loading config", e, tag = "ImageConfigResolver")
            } catch (e: IllegalArgumentException) {
                Napier.e("Invalid argument exception when loading config", e, tag = "ImageConfigResolver")
            }
        }
    }

    enum class ImageType {
        POSTER,
        BACKDROP,
        PROFILE,
        STILL
    }

    /**
     * Resolves the full URL for a TMDB image dynamically.
     *
     * @param path The relative image path (e.g., "/image.jpg")
     * @param type The TMDB image type bucket
     * @param targetWidthDp The rendering width in density-independent pixels (DP)
     * @param density The screen density
     */
    fun resolve(
        path: String?,
        type: ImageType,
        targetWidthDp: Int,
        density: Float
    ): String? {
        if (path.isNullOrEmpty()) return null
        val targetPixels = targetWidthDp * density
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        val sizes = when (type) {
            ImageType.POSTER -> activeConfig.poster_sizes
            ImageType.BACKDROP -> activeConfig.backdrop_sizes
            ImageType.PROFILE -> activeConfig.profile_sizes
            ImageType.STILL -> activeConfig.still_sizes
        }

        val resolvedSize = selectBestSize(sizes, targetPixels)
        return "${activeConfig.secure_base_url}$resolvedSize$cleanPath"
    }

    private fun selectBestSize(sizes: List<String>, targetPixels: Float): String {
        if (sizes.isEmpty()) return "original"
        
        // Map size names (e.g. "w185") to their numeric pixel widths
        val sizePairs = sizes.mapNotNull { size ->
            if (size.equals("original", ignoreCase = true)) {
                size to Float.MAX_VALUE
            } else {
                val numericPart = size.filter { it.isDigit() }.toIntOrNull()
                if (numericPart != null) {
                    size to numericPart.toFloat()
                } else {
                    null
                }
            }
        }

        // Find the smallest size that is greater than or equal to targetPixels
        val bestMatch = sizePairs
            .filter { it.second >= targetPixels }
            .minByOrNull { it.second }

        return bestMatch?.first ?: sizes.last()
    }
}
