package com.ajinkyabadve.kmmmywatchlist

import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences

actual fun getPlatformName(): String = "Desktop"

actual fun createSettings(): com.russhwolf.settings.Settings {
    val delegate = Preferences.userRoot().node("com.ajinkyabadve.kmmmywatchlist")
    return PreferencesSettings(delegate)
}
