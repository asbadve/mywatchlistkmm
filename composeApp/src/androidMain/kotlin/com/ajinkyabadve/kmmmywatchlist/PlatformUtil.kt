package com.ajinkyabadve.kmmmywatchlist

import android.content.Context
import android.content.pm.ApplicationInfo
import com.russhwolf.settings.SharedPreferencesSettings

actual fun getPlatformName(): String = "Android"

actual fun createSettings(): com.russhwolf.settings.Settings {
    val sharedPrefs = AndroidApp.instance.getSharedPreferences("watchlist_settings", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(sharedPrefs)
}

// Same manifest-driven check AndroidApp.onCreate() already gates initLogging() with, rather than
// a second mechanism (e.g. AGP's own BuildConfig.DEBUG) to keep in sync.
actual fun isDebugBuild(): Boolean = AndroidApp.instance.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
