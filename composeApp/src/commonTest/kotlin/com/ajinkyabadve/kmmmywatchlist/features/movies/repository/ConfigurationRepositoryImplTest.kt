package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.ConfigurationConstants
import com.ajinkyabadve.kmmmywatchlist.core.model.ImagesConfig
import com.ajinkyabadve.kmmmywatchlist.core.model.TmdbConfiguration
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.russhwolf.settings.Settings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurationRepositoryImplTest {

    // Simple Map-based fake settings implementation (mirrors ConfigurationRepositoryTest's FakeSettings).
    private class FakeSettings : Settings {
        private val stringMap = mutableMapOf<String, String>()
        private val longMap = mutableMapOf<String, Long>()
        val removedKeys = mutableListOf<String>()

        override val keys: Set<String> get() = stringMap.keys + longMap.keys
        override val size: Int get() = keys.size

        override fun hasKey(key: String): Boolean = keys.contains(key)
        override fun clear() {
            stringMap.clear()
            longMap.clear()
        }

        override fun remove(key: String) {
            removedKeys.add(key)
            stringMap.remove(key)
            longMap.remove(key)
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
        override fun getBooleanOrNull(key: String): Boolean? = null
        override fun getDouble(key: String, defaultValue: Double): Double = defaultValue
        override fun getDoubleOrNull(key: String): Double? = null
        override fun getFloat(key: String, defaultValue: Float): Float = defaultValue
        override fun getFloatOrNull(key: String): Float? = null
        override fun getInt(key: String, defaultValue: Int): Int = defaultValue
        override fun getIntOrNull(key: String): Int? = null
        override fun getLong(key: String, defaultValue: Long): Long = longMap[key] ?: defaultValue
        override fun getLongOrNull(key: String): Long? = longMap[key]
        override fun getString(key: String, defaultValue: String): String = stringMap[key] ?: defaultValue
        override fun getStringOrNull(key: String): String? = stringMap[key]

        override fun putBoolean(key: String, value: Boolean) {}
        override fun putDouble(key: String, value: Double) {}
        override fun putFloat(key: String, value: Float) {}
        override fun putInt(key: String, value: Int) {}
        override fun putLong(key: String, value: Long) {
            longMap[key] = value
        }
        override fun putString(key: String, value: String) {
            stringMap[key] = value
        }
    }

    private val fixtureConfig = ImagesConfig(
        base_url = "http://mock/",
        secure_base_url = "https://mock/",
        backdrop_sizes = listOf("w300"),
        logo_sizes = listOf("w45"),
        poster_sizes = listOf("w92"),
        profile_sizes = listOf("w45"),
        still_sizes = listOf("w92")
    )

    @Test
    fun testReturnsCachedConfigWithoutNetworkCallWhenFresh() = runTest {
        val settings = FakeSettings().apply {
            putString(ConfigurationConstants.KEY_CONFIG, Json.encodeToString(ImagesConfig.serializer(), fixtureConfig))
            putLong(ConfigurationConstants.KEY_TIMESTAMP, Clock.System.now().toEpochMilliseconds())
        }
        var networkCalls = 0
        val mockEngine = MockEngine {
            networkCalls++
            respond(content = "", status = HttpStatusCode.InternalServerError, headers = headersOf())
        }
        val repository = ConfigurationRepositoryImpl(TmdbClient(mockEngine), settings)

        val result = repository.getConfiguration()

        assertEquals(fixtureConfig, result)
        assertEquals(0, networkCalls)
    }

    @Test
    fun testFetchesAndCachesConfigWhenNoCacheExists() = runTest {
        val settings = FakeSettings()
        val mockEngine = MockEngine {
            respond(
                content = Json.encodeToString(TmdbConfiguration.serializer(), TmdbConfiguration(fixtureConfig)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val repository = ConfigurationRepositoryImpl(TmdbClient(mockEngine), settings)

        val result = repository.getConfiguration()

        assertEquals(fixtureConfig, result)
        assertEquals(
            Json.encodeToString(ImagesConfig.serializer(), fixtureConfig),
            settings.getString(ConfigurationConstants.KEY_CONFIG, "")
        )
    }

    @Test
    fun testRefetchesWhenCacheIsExpired() = runTest {
        val settings = FakeSettings().apply {
            putString(ConfigurationConstants.KEY_CONFIG, Json.encodeToString(ImagesConfig.serializer(), fixtureConfig))
            putLong(ConfigurationConstants.KEY_TIMESTAMP, Clock.System.now().toEpochMilliseconds() - 2 * ConfigurationConstants.DAY_IN_MILLIS)
        }
        var networkCalls = 0
        val mockEngine = MockEngine {
            networkCalls++
            respond(
                content = Json.encodeToString(TmdbConfiguration.serializer(), TmdbConfiguration(fixtureConfig)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val repository = ConfigurationRepositoryImpl(TmdbClient(mockEngine), settings)

        repository.getConfiguration()

        assertEquals(1, networkCalls)
    }

    @Test
    fun testClearsCorruptedCacheAndRefetchesFromNetwork() = runTest {
        val settings = FakeSettings().apply {
            putString(ConfigurationConstants.KEY_CONFIG, "{not valid json}")
            putLong(ConfigurationConstants.KEY_TIMESTAMP, Clock.System.now().toEpochMilliseconds())
        }
        val mockEngine = MockEngine {
            respond(
                content = Json.encodeToString(TmdbConfiguration.serializer(), TmdbConfiguration(fixtureConfig)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val repository = ConfigurationRepositoryImpl(TmdbClient(mockEngine), settings)

        val result = repository.getConfiguration()

        assertEquals(fixtureConfig, result)
        assertTrue(settings.removedKeys.contains(ConfigurationConstants.KEY_CONFIG))
    }

    @Test
    fun testFallsBackToDefaultConfigOnNetworkErrorWithNoCache() = runTest {
        val settings = FakeSettings()
        val mockEngine = MockEngine {
            respond(content = "", status = HttpStatusCode.InternalServerError, headers = headersOf())
        }
        val repository = ConfigurationRepositoryImpl(TmdbClient(mockEngine), settings)

        val result = repository.getConfiguration()

        assertEquals(ConfigurationConstants.defaultImagesConfig, result)
    }

    @Test
    fun testFallsBackToStaleCacheOnNetworkErrorWhenCacheExists() = runTest {
        val settings = FakeSettings().apply {
            putString(ConfigurationConstants.KEY_CONFIG, Json.encodeToString(ImagesConfig.serializer(), fixtureConfig))
            // Stale (expired) timestamp so the repository attempts a refetch rather than serving cache directly.
            putLong(ConfigurationConstants.KEY_TIMESTAMP, Clock.System.now().toEpochMilliseconds() - 2 * ConfigurationConstants.DAY_IN_MILLIS)
        }
        val mockEngine = MockEngine {
            respond(content = "", status = HttpStatusCode.InternalServerError, headers = headersOf())
        }
        val repository = ConfigurationRepositoryImpl(TmdbClient(mockEngine), settings)

        val result = repository.getConfiguration()

        assertEquals(fixtureConfig, result)
    }
}
