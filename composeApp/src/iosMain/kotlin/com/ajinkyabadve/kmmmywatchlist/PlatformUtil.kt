package com.ajinkyabadve.kmmmywatchlist

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

actual fun getPlatformName(): String = "iOS"

actual fun createSettings(): com.russhwolf.settings.Settings {
    return NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
}