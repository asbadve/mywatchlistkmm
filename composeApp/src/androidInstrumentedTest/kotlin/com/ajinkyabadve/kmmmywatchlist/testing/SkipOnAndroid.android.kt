package com.ajinkyabadve.kmmmywatchlist.testing

import org.junit.Assume

actual fun skipOnAndroid(reason: String) = Assume.assumeTrue(reason, false)
