package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

import java.security.GeneralSecurityException

internal actual suspend fun catchingAuthenticationFailure(block: suspend () -> ByteArray): ByteArray? =
    try {
        block()
    } catch (e: GeneralSecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
