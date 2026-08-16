package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.RestrictedModeConstant
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestrictedModeRepositoryImplTest {
    // Simple Map-based fake settings implementation (mirrors RegionRepositoryImplTest's FakeSettings).
    private class FakeSettings : Settings {
        private val booleanMap = mutableMapOf<String, Boolean>()

        override val keys: Set<String> get() = booleanMap.keys
        override val size: Int get() = keys.size

        override fun hasKey(key: String): Boolean = keys.contains(key)

        override fun clear() {
            booleanMap.clear()
        }

        override fun remove(key: String) {
            booleanMap.remove(key)
        }

        override fun getBoolean(
            key: String,
            defaultValue: Boolean,
        ): Boolean = booleanMap[key] ?: defaultValue

        override fun getBooleanOrNull(key: String): Boolean? = booleanMap[key]

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
        ): Long = defaultValue

        override fun getLongOrNull(key: String): Long? = null

        override fun getString(
            key: String,
            defaultValue: String,
        ): String = defaultValue

        override fun getStringOrNull(key: String): String? = null

        override fun putBoolean(
            key: String,
            value: Boolean,
        ) {
            booleanMap[key] = value
        }

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
        ) {}

        override fun putString(
            key: String,
            value: String,
        ) {}
    }

    @Test
    fun testDefaultsToTrueWhenNothingStored() {
        val repository = RestrictedModeRepositoryImpl(FakeSettings())

        assertTrue(repository.isRestrictedModeEnabled())
    }

    @Test
    fun testSetRestrictedModeEnabledPersistsAndIsReturnedByGet() {
        val settings = FakeSettings()
        val repository = RestrictedModeRepositoryImpl(settings)

        repository.setRestrictedModeEnabled(false)

        assertFalse(repository.isRestrictedModeEnabled())
        assertEquals(false, settings.getBoolean(RestrictedModeConstant.KEY_RESTRICTED_MODE_ENABLED, true))
    }

    @Test
    fun testTogglingBackToTrueIsPersisted() {
        val settings = FakeSettings()
        val repository = RestrictedModeRepositoryImpl(settings)

        repository.setRestrictedModeEnabled(false)
        repository.setRestrictedModeEnabled(true)

        assertTrue(repository.isRestrictedModeEnabled())
    }
}
