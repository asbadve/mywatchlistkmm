package com.ajinkyabadve.kmmmywatchlist

import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences

actual fun getPlatformName(): String = "Desktop"

actual fun isMobilePlatform(): Boolean = false

actual fun is24HourClock(): Boolean = false

actual fun createSettings(): com.russhwolf.settings.Settings {
    val delegate = Preferences.userRoot().node("com.ajinkyabadve.kmmmywatchlist")
    return PreferencesSettings(delegate)
}

// Desktop has no separate release distribution today (see the project's "Release build is
// benchmarking-only" guidance) - every desktop build is effectively a dev build.
actual fun isDebugBuild(): Boolean = true

// No JVM/desktop-wide "reduce motion" setting to read - see PlatformUtil.kt's kdoc.
actual fun isReducedMotionEnabled(): Boolean = false

// AWT/Swing expose no screen-reader-presence API - see PlatformUtil.kt's kdoc.
actual fun isScreenReaderActive(): Boolean = false

actual fun usesNativeAnimatedSplash(): Boolean = false
