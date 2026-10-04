package com.ajinkyabadve.kmmmywatchlist

expect fun getPlatformName(): String

expect fun createSettings(): com.russhwolf.settings.Settings

/** True only for a developer build (Android's debuggable flag; unconditionally true on iOS/
 *  desktop/JS, which this project has no separate release distribution pipeline for yet) - gates
 *  dev-only UI like AccountScreen's "Poll now" row so it can never reach a real release build. */
expect fun isDebugBuild(): Boolean

/** True if the user has asked for reduced/no animations - Android's "Remove animations" toggle
 *  (`ANIMATOR_DURATION_SCALE`), iOS's `UIAccessibilityIsReduceMotionEnabled`, or the browser's
 *  `prefers-reduced-motion` media query. Unconditionally `false` on desktop, which has no
 *  JVM/OS-wide equivalent to read. */
expect fun isReducedMotionEnabled(): Boolean

/** True while a screen reader is actively narrating the UI - Android's
 *  `AccessibilityManager.isTouchExplorationEnabled` (TalkBack) or iOS's
 *  `UIAccessibility.isVoiceOverRunning`. Unconditionally `false` on desktop and JS: AWT/Swing expose
 *  no such signal, and browsers deliberately don't let page script detect assistive technology. */
expect fun isScreenReaderActive(): Boolean

/** True on Android/iOS, where the OS can deliver a scheduled local notification with the app
 *  closed - false on desktop/web, which have no such scheduler. Gates all release-reminder UI
 *  (checklist item 16) so it never appears where a reminder couldn't actually fire. */
expect fun isMobilePlatform(): Boolean

/** True when the device shows times in 24-hour format (Android's system setting, iOS's locale /
 *  "24-Hour Time" toggle). Used to format the release-reminder time and configure its picker.
 *  Always `false` on desktop/JS, where reminder UI isn't shown. */
expect fun is24HourClock(): Boolean

/** True only on Android, where the native splash (`Theme.MyWatchList.Splash`,
 *  `splash_icon_animated.xml`) already plays the full "3b" icon reveal itself - `App()` skips
 *  `core.ui.splash.SplashScreen` entirely there instead of showing it as a second animation after
 *  the native one. iOS/Desktop/JS have no native animated-icon API to hand that reveal off to, so
 *  they keep showing the Compose splash. */
expect fun usesNativeAnimatedSplash(): Boolean
