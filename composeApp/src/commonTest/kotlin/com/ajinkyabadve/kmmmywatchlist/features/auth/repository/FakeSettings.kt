package com.ajinkyabadve.kmmmywatchlist.features.auth.repository

import com.russhwolf.settings.Settings

class FakeSettings : Settings {
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
