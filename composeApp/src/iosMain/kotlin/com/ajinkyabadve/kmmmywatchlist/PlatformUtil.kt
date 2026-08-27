package com.ajinkyabadve.kmmmywatchlist

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

actual fun getPlatformName(): String = "iOS"

actual fun createSettings(): com.russhwolf.settings.Settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)

// This project has no TestFlight/App Store release pipeline yet - every iOS build today is a dev
// build, so unlike Android there's no debuggable-flag equivalent worth wiring up.
actual fun isDebugBuild(): Boolean = true

actual fun isReducedMotionEnabled(): Boolean = UIAccessibilityIsReduceMotionEnabled()

actual fun usesNativeAnimatedSplash(): Boolean = false
