package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

internal actual suspend fun catchingAuthenticationFailure(block: suspend () -> ByteArray): ByteArray? =
    try {
        block()
    } catch (e: IllegalStateException) {
        // CryptoKit's authentication failure ("CryptoKitError error 3"), as cryptography-kotlin surfaces it.
        null
    } catch (e: IllegalArgumentException) {
        null
    }
