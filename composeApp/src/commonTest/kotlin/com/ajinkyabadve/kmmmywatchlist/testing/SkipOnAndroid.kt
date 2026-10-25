package com.ajinkyabadve.kmmmywatchlist.testing

/**
 * Marks the calling test as skipped when it runs on Android (a JUnit assumption, so the report says
 * "skipped", not "passed") and does nothing on every other platform. Only for failures that are
 * Android framework bugs outside this app - always pass the issue link as [reason].
 */
expect fun skipOnAndroid(reason: String)
