package com.ajinkyabadve.kmmmywatchlist

import com.russhwolf.settings.StorageSettings
import kotlinx.browser.localStorage
import kotlinx.browser.window

actual fun getPlatformName(): String = "Browser"

actual fun isMobilePlatform(): Boolean = false

actual fun is24HourClock(): Boolean = false

private val defaultSettings: com.russhwolf.settings.Settings by lazy {
    StorageSettings(localStorage)
}

actual fun createSettings(): com.russhwolf.settings.Settings = defaultSettings

// This project has no separate web release/CDN pipeline yet - every JS build today is a dev build.
actual fun isDebugBuild(): Boolean = true

actual fun isReducedMotionEnabled(): Boolean = window.matchMedia("(prefers-reduced-motion: reduce)").matches

// Browsers deliberately don't expose assistive-technology presence to page script - see
// PlatformUtil.kt's kdoc.
actual fun isScreenReaderActive(): Boolean = false

actual fun usesNativeAnimatedSplash(): Boolean = false

actual fun supportsLocalBackup(): Boolean = false
