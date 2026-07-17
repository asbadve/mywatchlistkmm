package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.ConfigurationConstants
import com.ajinkyabadve.kmmmywatchlist.core.model.ImagesConfig
import com.ajinkyabadve.kmmmywatchlist.core.model.TmdbConfiguration
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import com.russhwolf.settings.Settings
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.utils.io.errors.IOException
import kotlinproject.composeapp.BuildConfig
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface ConfigurationRepository {
    suspend fun getConfiguration(): ImagesConfig
}

class ConfigurationRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
    private val settings: Settings = com.ajinkyabadve.kmmmywatchlist.createSettings(),
) : ConfigurationRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getConfiguration(): ImagesConfig {
        val cachedJson = settings.getString(ConfigurationConstants.KEY_CONFIG, "")
        val lastFetch = settings.getLong(ConfigurationConstants.KEY_TIMESTAMP, 0L)
        val now = Clock.System.now().toEpochMilliseconds()

        // If cache is fresh, parse and return
        if (cachedJson.isNotEmpty() && (now - lastFetch < ConfigurationConstants.DAY_IN_MILLIS)) {
            try {
                return json.decodeFromString(ImagesConfig.serializer(), cachedJson)
            } catch (e: SerializationException) {
                // Settings payload is corrupted; clear setting and force fresh fetch
                settings.remove(ConfigurationConstants.KEY_CONFIG)
            } catch (e: IllegalArgumentException) {
                // In case of class model schema changes/mismatch
                settings.remove(ConfigurationConstants.KEY_CONFIG)
            }
        }

        // Fetch from network
        try {
            val response: HttpResponse =
                tmdbClient.client.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = NetworkConstant.HOST
                        trailingQuery = true
                        encodedPath = "/3/configuration"
                        parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    }
                }
            val config: TmdbConfiguration = response.body()
            val imagesConfig = config.images

            // Save cache
            settings.putString(
                ConfigurationConstants.KEY_CONFIG,
                json.encodeToString(
                    ImagesConfig.serializer(),
                    imagesConfig,
                ),
            )
            settings.putLong(ConfigurationConstants.KEY_TIMESTAMP, now)

            return imagesConfig
        } catch (e: ResponseException) {
            // Server error response (e.g. 401, 404, 500)
            return getFallbackConfig(cachedJson)
        } catch (e: ConnectTimeoutException) {
            // Connection timeout
            return getFallbackConfig(cachedJson)
        } catch (e: HttpRequestTimeoutException) {
            // Request timeout
            return getFallbackConfig(cachedJson)
        } catch (e: IOException) {
            // General network IO / Offline state
            return getFallbackConfig(cachedJson)
        }
    }

    private fun getFallbackConfig(cachedJson: String): ImagesConfig {
        if (cachedJson.isNotEmpty()) {
            try {
                return json.decodeFromString(ImagesConfig.serializer(), cachedJson)
            } catch (e: SerializationException) {
                // Ignore corrupted caches
            } catch (e: IllegalArgumentException) {
                // Ignore
            }
        }
        return ConfigurationConstants.defaultImagesConfig
    }
}
