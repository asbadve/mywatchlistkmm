package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom

/**
 * AES-256-GCM under a key derived from the ticket's one-time secret (item 15 stage 2). The IV is random per message
 * and travels prepended to the ciphertext. Associated data binds a message to its direction, so a
 * frame can't be reflected back to the side that sent it.
 */
class TransferCipher private constructor(
    private val key: AES.GCM.Key,
) {
    suspend fun encrypt(
        plaintext: ByteArray,
        associatedData: ByteArray,
    ): ByteArray = key.cipher().encrypt(plaintext, associatedData)

    /** Null when the message wasn't encrypted with this key, was tampered with, or is truncated. */
    suspend fun decryptOrNull(
        ciphertext: ByteArray,
        associatedData: ByteArray,
    ): ByteArray? = catchingAuthenticationFailure { key.cipher().decrypt(ciphertext, associatedData) }

    companion object {
        private val aesGcm get() = CryptographyProvider.Default.get(AES.GCM)

        /**
         * SHA-256 over a fixed label and the secret. The secret is uniformly random, so a plain hash
         * is a sound key derivation here (HKDF would add nothing without a salt to mix in); the label
         * keeps this key from ever equalling a hash of the same bytes used elsewhere.
         */
        suspend fun fromSecret(secret: ByteArray): TransferCipher {
            val rawKey =
                CryptographyProvider.Default
                    .get(SHA256)
                    .hasher()
                    .hash(TransferCipherConstant.KEY_LABEL + secret)
            return TransferCipher(aesGcm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, rawKey))
        }

        /** A fresh one-time secret for a ticket. */
        fun generateSecret(): ByteArray = CryptographyRandom.Default.nextBytes(TransferTicketConstant.SECRET_BYTES)
    }
}

/**
 * Runs a decrypt and maps an authentication failure to null. Per platform because the crypto
 * backends report it differently (checked 2026-10-04 against cryptography-kotlin 0.6.0): the JDK
 * throws `AEADBadTagException` (a `GeneralSecurityException`), CryptoKit on iOS an
 * `IllegalStateException`; both throw `IllegalArgumentException` for a truncated message.
 */
internal expect suspend fun catchingAuthenticationFailure(block: suspend () -> ByteArray): ByteArray?

private object TransferCipherConstant {
    val KEY_LABEL = "mywatchlist-transfer-v1:key".encodeToByteArray()
}
