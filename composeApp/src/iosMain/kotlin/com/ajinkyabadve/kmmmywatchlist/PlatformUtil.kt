package com.ajinkyabadve.kmmmywatchlist

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAccessibilityIsVoiceOverRunning

actual fun getPlatformName(): String = "iOS"

actual fun isMobilePlatform(): Boolean = true

// The locale's preferred hour pattern ("j" skeleton) has no "a" (AM/PM marker) in 24-hour mode -
// this also honours the user's "24-Hour Time" toggle, which iOS applies to the current locale.
actual fun is24HourClock(): Boolean =
    NSDateFormatter
        .dateFormatFromTemplate("j", 0u, NSLocale.currentLocale)
        ?.contains("a") == false

actual fun createSettings(): com.russhwolf.settings.Settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)

// This project has no TestFlight/App Store release pipeline yet - every iOS build today is a dev
// build, so unlike Android there's no debuggable-flag equivalent worth wiring up.
actual fun isDebugBuild(): Boolean = true

actual fun isReducedMotionEnabled(): Boolean = UIAccessibilityIsReduceMotionEnabled()

actual fun isScreenReaderActive(): Boolean = UIAccessibilityIsVoiceOverRunning()

actual fun usesNativeAnimatedSplash(): Boolean = false

actual fun supportsLocalBackup(): Boolean = true
