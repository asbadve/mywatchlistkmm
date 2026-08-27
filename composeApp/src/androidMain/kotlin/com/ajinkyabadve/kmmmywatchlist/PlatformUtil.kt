package com.ajinkyabadve.kmmmywatchlist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual fun getPlatformName(): String = "Android"

actual fun createSettings(): com.russhwolf.settings.Settings {
    val sharedPrefs = AndroidApp.instance.getSharedPreferences("watchlist_settings", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(sharedPrefs)
}

// Same manifest-driven check AndroidApp.onCreate() already gates initLogging() with, rather than
// a second mechanism (e.g. AGP's own BuildConfig.DEBUG) to keep in sync.
actual fun isDebugBuild(): Boolean = AndroidApp.instance.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

// 0f is exactly what "Remove animations" (Settings > Accessibility) sets this to; anything else -
// including the 0.5x/2x speed-up/slow-down options - leaves animations on, just at a different rate.
actual fun isReducedMotionEnabled(): Boolean =
    Settings.Global.getFloat(AndroidApp.instance.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

actual fun usesNativeAnimatedSplash(): Boolean = true
