package com.ajinkyabadve.kmmmywatchlist

import com.russhwolf.settings.StorageSettings
import kotlinx.browser.localStorage

actual fun getPlatformName(): String = "Browser"

actual fun createSettings(): com.russhwolf.settings.Settings = StorageSettings(localStorage)
