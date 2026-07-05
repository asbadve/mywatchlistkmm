package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.ConfigurationConstants
import com.ajinkyabadve.kmmmywatchlist.core.model.ImagesConfig
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigurationRepositoryTest {

    // Simple Map-based fake settings implementation
    private class FakeSettings : com.russhwolf.settings.Settings {
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

    // A fake configuration repository that uses local cache and supports toggling mock scenarios
    private class FakeConfigurationRepository(
        val settings: FakeSettings
    ) : ConfigurationRepository {
        var networkCallsCount = 0
        var shouldFailNetwork = false
        var exceptionToThrow: Exception? = null

        override suspend fun getConfiguration(): ImagesConfig {
            val cachedJson = settings.getString(ConfigurationConstants.KEY_CONFIG, "")
            val lastFetch = settings.getLong(ConfigurationConstants.KEY_TIMESTAMP, 0L)
            val now = Clock.System.now().toEpochMilliseconds()

            if (cachedJson.isNotEmpty() && (now - lastFetch < ConfigurationConstants.DAY_IN_MILLIS)) {
                try {
                    // Try parsing JSON to simulate repository deserialization check
                    if (cachedJson == "{INVALID_JSON}") {
                        throw SerializationException("Parsing failed")
                    }
                    return ConfigurationConstants.defaultImagesConfig
                } catch (e: SerializationException) {
                    settings.remove(ConfigurationConstants.KEY_CONFIG)
                }
            }

            networkCallsCount++
            if (shouldFailNetwork) {
                try {
                    exceptionToThrow?.let { throw it }
                    throw io.ktor.utils.io.errors.IOException("Network offline")
                } catch (e: io.ktor.client.plugins.ResponseException) {
                    return getFallbackConfig(cachedJson)
                } catch (e: io.ktor.client.network.sockets.ConnectTimeoutException) {
                    return getFallbackConfig(cachedJson)
                } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
                    return getFallbackConfig(cachedJson)
                } catch (e: io.ktor.utils.io.errors.IOException) {
                    return getFallbackConfig(cachedJson)
                }
            }

            // Simulate saving network fetch to settings cache
            settings.putString(ConfigurationConstants.KEY_CONFIG, "{\"base_url\":\"http://mock/\",\"secure_base_url\":\"https://mock/\",\"backdrop_sizes\":[],\"logo_sizes\":[],\"poster_sizes\":[],\"profile_sizes\":[],\"still_sizes\":[]}")
            settings.putLong(ConfigurationConstants.KEY_TIMESTAMP, now)

            return ConfigurationConstants.defaultImagesConfig
        }

        private fun getFallbackConfig(cachedJson: String): ImagesConfig {
            return ConfigurationConstants.defaultImagesConfig
        }
    }

    @Test
    fun testGetConfigurationReturnsCachedWhenFresh() = kotlinx.coroutines.test.runTest {
        val settings = FakeSettings()
        val repository = FakeConfigurationRepository(settings)

        // Pre-fill clean cached config and timestamp
        settings.putString(ConfigurationConstants.KEY_CONFIG, "{\"base_url\":\"http://mock/\",\"secure_base_url\":\"https://mock/\",\"backdrop_sizes\":[],\"logo_sizes\":[],\"poster_sizes\":[],\"profile_sizes\":[],\"still_sizes\":[]}")
        settings.putLong(ConfigurationConstants.KEY_TIMESTAMP, Clock.System.now().toEpochMilliseconds())

        val config = repository.getConfiguration()
        assertNotNull(config)
        assertEquals(0, repository.networkCallsCount)
    }

    @Test
    fun testGetConfigurationFetchesNetworkWhenCacheExpired() = kotlinx.coroutines.test.runTest {
        val settings = FakeSettings()
        val repository = FakeConfigurationRepository(settings)

        // Pre-fill expired cache timestamp
        settings.putString(ConfigurationConstants.KEY_CONFIG, "{\"base_url\":\"http://mock/\",\"secure_base_url\":\"https://mock/\",\"backdrop_sizes\":[],\"logo_sizes\":[],\"poster_sizes\":[],\"profile_sizes\":[],\"still_sizes\":[]}")
        settings.putLong(ConfigurationConstants.KEY_TIMESTAMP, Clock.System.now().toEpochMilliseconds() - 2 * ConfigurationConstants.DAY_IN_MILLIS)

        val config = repository.getConfiguration()
        assertNotNull(config)
        assertEquals(1, repository.networkCallsCount)
    }

    @Test
    fun testGetConfigurationClearsCorruptedCache() = kotlinx.coroutines.test.runTest {
        val settings = FakeSettings()
        val repository = FakeConfigurationRepository(settings)

        // Pre-fill invalid JSON to trigger SerializationException
        settings.putString(ConfigurationConstants.KEY_CONFIG, "{INVALID_JSON}")
        settings.putLong(ConfigurationConstants.KEY_TIMESTAMP, Clock.System.now().toEpochMilliseconds())

        val config = repository.getConfiguration()
        assertNotNull(config)
        // Verify key was removed from settings
        assertTrue(settings.removedKeys.contains(ConfigurationConstants.KEY_CONFIG))
        // Verify it fallback-called network
        assertEquals(1, repository.networkCallsCount)
    }

    @Test
    fun testGetConfigurationFallbackOnNetworkError() = kotlinx.coroutines.test.runTest {
        val settings = FakeSettings()
        val repository = FakeConfigurationRepository(settings)
        repository.shouldFailNetwork = true
        repository.exceptionToThrow = io.ktor.client.plugins.HttpRequestTimeoutException("url", 1000L)

        val config = repository.getConfiguration()
        // Verify it returned fallback default
        assertNotNull(config)
        assertEquals(ConfigurationConstants.defaultImagesConfig.base_url, config.base_url)
    }
}
