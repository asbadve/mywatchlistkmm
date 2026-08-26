package com.ajinkyabadve.kmmmywatchlist

expect fun getPlatformName(): String

expect fun createSettings(): com.russhwolf.settings.Settings

/** True only for a developer build (Android's debuggable flag; unconditionally true on iOS/
 *  desktop/JS, which this project has no separate release distribution pipeline for yet) - gates
 *  dev-only UI like AccountScreen's "Poll now" row so it can never reach a real release build. */
expect fun isDebugBuild(): Boolean
