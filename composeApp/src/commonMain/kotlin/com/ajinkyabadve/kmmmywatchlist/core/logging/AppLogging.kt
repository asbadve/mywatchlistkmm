package com.ajinkyabadve.kmmmywatchlist.core.logging

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

private var isInitialised = false

/**
 * Installs a Napier backend so log output actually goes somewhere.
 *
 * Napier drops every call when no antilog is registered, so without this the client's
 * `install(Logging) { level = LogLevel.ALL }` and every `Napier.d`/`log { }` in the screen models
 * produced no output at all - diagnosing a network failure meant reaching for `adb`/`nc` instead
 * of reading logcat.
 *
 * Call once per process from each platform's entry point. Platforms decide for themselves whether
 * to call it: [Napier.base] appends, so calling twice would duplicate every line, hence the guard.
 */
fun initLogging() {
    if (isInitialised) return
    isInitialised = true
    Napier.base(DebugAntilog())
}
