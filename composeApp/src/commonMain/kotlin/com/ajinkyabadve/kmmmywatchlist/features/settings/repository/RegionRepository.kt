package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

import androidx.compose.ui.text.intl.Locale
import com.ajinkyabadve.kmmmywatchlist.core.constant.RegionConstant
import com.ajinkyabadve.kmmmywatchlist.features.settings.model.WatchProviderRegion
import com.ajinkyabadve.kmmmywatchlist.features.settings.model.WatchProviderRegionsResponse
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface RegionRepository {
    suspend fun getAvailableRegions(): List<WatchProviderRegion>

    fun getSelectedRegion(): String

    fun setSelectedRegion(regionCode: String)

    fun getFallbackRegion(): String

    fun setFallbackRegion(regionCode: String)
}

class RegionRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
    private val settings: Settings = com.ajinkyabadve.kmmmywatchlist.createSettings(),
) : RegionRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val regionListSerializer = ListSerializer(WatchProviderRegion.serializer())

    override suspend fun getAvailableRegions(): List<WatchProviderRegion> {
        val cachedJson = settings.getString(RegionConstant.KEY_REGIONS_CACHE, "")
        val lastFetch = settings.getLong(RegionConstant.KEY_REGIONS_TIMESTAMP, 0L)
        val now = Clock.System.now().toEpochMilliseconds()

        if (cachedJson.isNotEmpty() && ((now - lastFetch) < RegionConstant.REGIONS_CACHE_TTL_MILLIS)) {
            try {
                return json.decodeFromString(regionListSerializer, cachedJson)
            } catch (e: SerializationException) {
                Napier.w(tag = TAG, throwable = e) { "Corrupted cached regions JSON, clearing cache" }
                settings.remove(RegionConstant.KEY_REGIONS_CACHE)
            } catch (e: IllegalArgumentException) {
                Napier.w(tag = TAG, throwable = e) { "Cached regions schema mismatch, clearing cache" }
                settings.remove(RegionConstant.KEY_REGIONS_CACHE)
            }
        }

        return try {
            val response: HttpResponse =
                tmdbClient.client.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = NetworkConstant.HOST
                        trailingQuery = true
                        encodedPath = "/3/watch/providers/regions"
                        parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    }
                }
            val regions = response.body<WatchProviderRegionsResponse>().results

            settings.putString(RegionConstant.KEY_REGIONS_CACHE, json.encodeToString(regionListSerializer, regions))
            settings.putLong(RegionConstant.KEY_REGIONS_TIMESTAMP, now)

            regions
        } catch (e: ResponseException) {
            Napier.w(tag = TAG, throwable = e) { "Server error fetching regions, using fallback" }
            getFallbackRegions(cachedJson)
        } catch (e: ConnectTimeoutException) {
            Napier.w(tag = TAG, throwable = e) { "Connection timeout fetching regions, using fallback" }
            getFallbackRegions(cachedJson)
        } catch (e: HttpRequestTimeoutException) {
            Napier.w(tag = TAG, throwable = e) { "Request timeout fetching regions, using fallback" }
            getFallbackRegions(cachedJson)
        } catch (e: IOException) {
            Napier.w(tag = TAG, throwable = e) { "Network IO error fetching regions, using fallback" }
            getFallbackRegions(cachedJson)
        }
    }

    override fun getSelectedRegion(): String {
        val stored = settings.getString(RegionConstant.KEY_SELECTED_REGION, "")
        if (stored.isNotEmpty()) return stored
        val deviceRegion = Locale.current.region.uppercase()
        return deviceRegion.ifEmpty { RegionConstant.US }
    }

    override fun setSelectedRegion(regionCode: String) {
        settings.putString(RegionConstant.KEY_SELECTED_REGION, regionCode)
    }

    override fun getFallbackRegion(): String = settings.getString(RegionConstant.KEY_FALLBACK_REGION, RegionConstant.US)

    override fun setFallbackRegion(regionCode: String) {
        settings.putString(RegionConstant.KEY_FALLBACK_REGION, regionCode)
    }

    private fun getFallbackRegions(cachedJson: String): List<WatchProviderRegion> {
        if (cachedJson.isNotEmpty()) {
            try {
                return json.decodeFromString(regionListSerializer, cachedJson)
            } catch (e: SerializationException) {
                Napier.w(tag = TAG, throwable = e) { "Corrupted cached regions JSON, using empty fallback" }
            } catch (e: IllegalArgumentException) {
                Napier.w(tag = TAG, throwable = e) { "Cached regions schema mismatch, using empty fallback" }
            }
        }
        return emptyList()
    }

    private companion object {
        const val TAG = "RegionRepositoryImpl"
    }
}
