package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.RegionConstant
import com.ajinkyabadve.kmmmywatchlist.features.settings.model.WatchProviderRegion
import com.ajinkyabadve.kmmmywatchlist.features.settings.model.WatchProviderRegionsResponse
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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegionRepositoryImplTest {
    // Simple Map-based fake settings implementation (mirrors ConfigurationRepositoryImplTest's FakeSettings).
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

        override fun getBoolean(
            key: String,
            defaultValue: Boolean,
        ): Boolean = defaultValue

        override fun getBooleanOrNull(key: String): Boolean? = null

        override fun getDouble(
            key: String,
            defaultValue: Double,
        ): Double = defaultValue

        override fun getDoubleOrNull(key: String): Double? = null

        override fun getFloat(
            key: String,
            defaultValue: Float,
        ): Float = defaultValue

        override fun getFloatOrNull(key: String): Float? = null

        override fun getInt(
            key: String,
            defaultValue: Int,
        ): Int = defaultValue

        override fun getIntOrNull(key: String): Int? = null

        override fun getLong(
            key: String,
            defaultValue: Long,
        ): Long = longMap[key] ?: defaultValue

        override fun getLongOrNull(key: String): Long? = longMap[key]

        override fun getString(
            key: String,
            defaultValue: String,
        ): String = stringMap[key] ?: defaultValue

        override fun getStringOrNull(key: String): String? = stringMap[key]

        override fun putBoolean(
            key: String,
            value: Boolean,
        ) {}

        override fun putDouble(
            key: String,
            value: Double,
        ) {}

        override fun putFloat(
            key: String,
            value: Float,
        ) {}

        override fun putInt(
            key: String,
            value: Int,
        ) {}

        override fun putLong(
            key: String,
            value: Long,
        ) {
            longMap[key] = value
        }

        override fun putString(
            key: String,
            value: String,
        ) {
            stringMap[key] = value
        }
    }

    private val regionListSerializer = ListSerializer(WatchProviderRegion.serializer())
    private val fixtureRegions =
        listOf(
            WatchProviderRegion(iso3166 = "US", englishName = "United States", nativeName = "United States"),
            WatchProviderRegion(iso3166 = "IN", englishName = "India", nativeName = "India"),
        )

    @Test
    fun testGetSelectedRegionReturnsStoredValueWhenPresent() {
        val settings = FakeSettings().apply { putString(RegionConstant.KEY_SELECTED_REGION, "FR") }
        val repository = RegionRepositoryImpl(TmdbClient(MockEngine { respond(content = "") }), settings)

        assertEquals("FR", repository.getSelectedRegion())
    }

    @Test
    fun testGetSelectedRegionFallsBackToDeviceRegionWhenNothingStored() {
        val settings = FakeSettings()
        val repository = RegionRepositoryImpl(TmdbClient(MockEngine { respond(content = "") }), settings)

        val result = repository.getSelectedRegion()

        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testSetSelectedRegionPersistsAndIsReturnedByGet() {
        val settings = FakeSettings()
        val repository = RegionRepositoryImpl(TmdbClient(MockEngine { respond(content = "") }), settings)

        repository.setSelectedRegion("JP")

        assertEquals("JP", repository.getSelectedRegion())
        assertEquals("JP", settings.getString(RegionConstant.KEY_SELECTED_REGION, ""))
    }

    @Test
    fun testGetFallbackRegionDefaultsToUsWhenNothingStored() {
        val settings = FakeSettings()
        val repository = RegionRepositoryImpl(TmdbClient(MockEngine { respond(content = "") }), settings)

        assertEquals(RegionConstant.US, repository.getFallbackRegion())
    }

    @Test
    fun testSetFallbackRegionPersistsAndIsReturnedByGet() {
        val settings = FakeSettings()
        val repository = RegionRepositoryImpl(TmdbClient(MockEngine { respond(content = "") }), settings)

        repository.setFallbackRegion("FR")

        assertEquals("FR", repository.getFallbackRegion())
        assertEquals("FR", settings.getString(RegionConstant.KEY_FALLBACK_REGION, ""))
    }

    @Test
    fun testReturnsCachedRegionsWithoutNetworkCallWhenFresh() =
        runTest {
            val settings =
                FakeSettings().apply {
                    putString(RegionConstant.KEY_REGIONS_CACHE, Json.encodeToString(regionListSerializer, fixtureRegions))
                    putLong(RegionConstant.KEY_REGIONS_TIMESTAMP, Clock.System.now().toEpochMilliseconds())
                }
            var networkCalls = 0
            val mockEngine =
                MockEngine {
                    networkCalls++
                    respond(content = "", status = HttpStatusCode.InternalServerError, headers = headersOf())
                }
            val repository = RegionRepositoryImpl(TmdbClient(mockEngine), settings)

            val result = repository.getAvailableRegions()

            assertEquals(fixtureRegions, result)
            assertEquals(0, networkCalls)
        }

    @Test
    fun testFetchesAndCachesRegionsWhenNoCacheExists() =
        runTest {
            val settings = FakeSettings()
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            Json.encodeToString(
                                WatchProviderRegionsResponse.serializer(),
                                WatchProviderRegionsResponse(fixtureRegions),
                            ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = RegionRepositoryImpl(TmdbClient(mockEngine), settings)

            val result = repository.getAvailableRegions()

            assertEquals(fixtureRegions, result)
            assertEquals(
                Json.encodeToString(regionListSerializer, fixtureRegions),
                settings.getString(RegionConstant.KEY_REGIONS_CACHE, ""),
            )
        }

    @Test
    fun testRefetchesWhenCacheIsExpired() =
        runTest {
            val settings =
                FakeSettings().apply {
                    putString(RegionConstant.KEY_REGIONS_CACHE, Json.encodeToString(regionListSerializer, fixtureRegions))
                    putLong(
                        RegionConstant.KEY_REGIONS_TIMESTAMP,
                        Clock.System.now().toEpochMilliseconds() - 2 * RegionConstant.REGIONS_CACHE_TTL_MILLIS,
                    )
                }
            var networkCalls = 0
            val mockEngine =
                MockEngine {
                    networkCalls++
                    respond(
                        content =
                            Json.encodeToString(
                                WatchProviderRegionsResponse.serializer(),
                                WatchProviderRegionsResponse(fixtureRegions),
                            ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = RegionRepositoryImpl(TmdbClient(mockEngine), settings)

            repository.getAvailableRegions()

            assertEquals(1, networkCalls)
        }

    @Test
    fun testFallsBackToEmptyListOnNetworkErrorWithNoCache() =
        runTest {
            val settings = FakeSettings()
            val mockEngine =
                MockEngine {
                    respond(content = "", status = HttpStatusCode.InternalServerError, headers = headersOf())
                }
            val repository = RegionRepositoryImpl(TmdbClient(mockEngine), settings)

            val result = repository.getAvailableRegions()

            assertEquals(emptyList(), result)
        }

    @Test
    fun testFallsBackToStaleCacheOnNetworkErrorWhenCacheExists() =
        runTest {
            val settings =
                FakeSettings().apply {
                    putString(RegionConstant.KEY_REGIONS_CACHE, Json.encodeToString(regionListSerializer, fixtureRegions))
                    putLong(
                        RegionConstant.KEY_REGIONS_TIMESTAMP,
                        Clock.System.now().toEpochMilliseconds() - 2 * RegionConstant.REGIONS_CACHE_TTL_MILLIS,
                    )
                }
            val mockEngine =
                MockEngine {
                    respond(content = "", status = HttpStatusCode.InternalServerError, headers = headersOf())
                }
            val repository = RegionRepositoryImpl(TmdbClient(mockEngine), settings)

            val result = repository.getAvailableRegions()

            assertEquals(fixtureRegions, result)
        }
}
