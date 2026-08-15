package com.ajinkyabadve.kmmmywatchlist

private object PlatformConstant {
    // The exact strings the per-platform `getPlatformName` actuals return. Behaviour is keyed off
    // these, so they are a contract rather than display text - change one and its actual together.
    const val ANDROID = "Android"
    const val DESKTOP = "Desktop"
}

/**
 * Platform checks for the handful of places where behaviour genuinely differs.
 *
 * Deliberately derived from [getPlatformName] rather than declared as their own `expect val`: one
 * platform discriminator with four actuals is enough, and a second parallel one would only be more
 * files to keep in step. Reach for a real `expect`/`actual` when a platform needs different *code*,
 * not when it needs a different answer to a yes/no question.
 *
 * These live here rather than beside the `expect`s in `PlatformUtil.kt` because a common file
 * holding only `expect` declarations generates no JVM class, while one with real top-level members
 * does - and that collides with the same-named actual files ("Duplicate JVM class name
 * PlatformUtilKt").
 */
internal val isAndroid: Boolean get() = getPlatformName() == PlatformConstant.ANDROID

internal val isDesktop: Boolean get() = getPlatformName() == PlatformConstant.DESKTOP
