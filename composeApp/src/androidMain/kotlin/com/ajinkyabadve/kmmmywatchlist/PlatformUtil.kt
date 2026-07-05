package com.ajinkyabadve.kmmmywatchlist

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

actual fun getPlatformName(): String = "Android"

actual fun createSettings(): com.russhwolf.settings.Settings {
    val sharedPrefs = AndroidApp.INSTANCE.getSharedPreferences("watchlist_settings", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(sharedPrefs)
}
