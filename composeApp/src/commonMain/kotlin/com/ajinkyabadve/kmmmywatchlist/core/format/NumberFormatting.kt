package com.ajinkyabadve.kmmmywatchlist.core.format

private object NumberFormatConstant {
    const val ONE_DECIMAL_SCALE = 10
}

/**
 * One decimal place, without a platform number formatter - `kotlin.text` has no common-code
 * equivalent of `%.1f`, so ratings and popularity scores were each rounding by hand.
 */
fun Double.toOneDecimalString(): String {
    val scaled = (this * NumberFormatConstant.ONE_DECIMAL_SCALE).toInt()
    return "${scaled / NumberFormatConstant.ONE_DECIMAL_SCALE}.${scaled % NumberFormatConstant.ONE_DECIMAL_SCALE}"
}
