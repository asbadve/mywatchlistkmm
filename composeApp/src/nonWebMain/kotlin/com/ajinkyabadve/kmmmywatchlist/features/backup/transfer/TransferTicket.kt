package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Where to connect and the one-time secret the transfer's key is derived from (item 15 stage 2).
 * Possession of a fresh ticket *is* the pairing - no account, no PIN - so it is short-lived and
 * only ever accepted once.
 *
 * Two encodings of the same ticket:
 * - **QR code** - `mwlxfer1;<ipv4>;<port>;<base64url secret>;<expiry epoch seconds>`.
 * - **Short code** for typing (a desktop can't scan) - 20 Crockford Base32 characters,
 *   `7K3M-Q9XA-4RTB-W2HN-8CFD`, packing the last two IPv4 octets, the port and the secret. The
 *   receiver supplies the first two octets from its own address (both devices share the Wi-Fi), and
 *   there is no expiry in it: the sender enforces that by closing its socket.
 *
 * The secret is 64 bits - short enough to type, and guessing it offline would take centuries, while
 * a code lives 5 minutes, works once, and the backup holds no credentials by design.
 */
class TransferTicket(
    val host: String,
    val port: Int,
    val secret: ByteArray,
    val expiresAtEpochSeconds: Long,
) {
    fun isExpired(nowEpochSeconds: Long): Boolean = nowEpochSeconds >= expiresAtEpochSeconds

    @OptIn(ExperimentalEncodingApi::class)
    fun encode(): String =
        listOf(TransferTicketConstant.PREFIX, host, port, Base64.UrlSafe.encode(secret), expiresAtEpochSeconds)
            .joinToString(TransferTicketConstant.SEPARATOR)

    /** The typeable form, grouped in fours. */
    fun toShortCode(): String {
        val octets = host.split(TransferTicketConstant.OCTET_SEPARATOR).map { it.toInt() }
        val bytes =
            byteArrayOf(
                octets[2].toByte(),
                octets[3].toByte(),
                (port shr TransferTicketConstant.BITS_PER_BYTE).toByte(),
                port.toByte(),
            ) + secret
        return CrockfordBase32
            .encode(bytes)
            .chunked(TransferTicketConstant.SHORT_CODE_GROUP)
            .joinToString(TransferTicketConstant.SHORT_CODE_GROUP_SEPARATOR)
    }

    companion object {
        /** Null for anything that isn't a well-formed QR ticket - a random QR code, a URL, a truncated scan. */
        @OptIn(ExperimentalEncodingApi::class)
        fun decode(text: String): TransferTicket? {
            val parts = text.trim().split(TransferTicketConstant.SEPARATOR)
            if (parts.size != TransferTicketConstant.PART_COUNT || parts[0] != TransferTicketConstant.PREFIX) return null
            val host = parts[1].takeIf { TransferTicketConstant.IPV4.matches(it) } ?: return null
            val port = parts[2].toIntOrNull()?.takeIf { it in TransferTicketConstant.PORT_RANGE } ?: return null
            val secret =
                try {
                    Base64.UrlSafe.decode(parts[3])
                } catch (e: IllegalArgumentException) {
                    return null
                }
            if (secret.size != TransferTicketConstant.SECRET_BYTES) return null
            val expiresAt = parts[4].toLongOrNull() ?: return null
            return TransferTicket(host, port, secret, expiresAt)
        }

        /**
         * Null unless [text] is a well-formed short code. Forgiving about how it was typed: case,
         * dashes, spaces, and the letters Crockford Base32 treats as digits (O for 0, I/L for 1).
         * [receiverIpv4] supplies the first two octets of the sender's address.
         */
        fun fromShortCode(
            text: String,
            receiverIpv4: String,
        ): TransferTicket? {
            val bytes = CrockfordBase32.decode(text) ?: return null
            if (bytes.size != TransferTicketConstant.SHORT_CODE_BYTES) return null
            val prefix = receiverIpv4.split(TransferTicketConstant.OCTET_SEPARATOR).take(2)
            if (prefix.size != 2) return null
            val host =
                (prefix + listOf(bytes[0].toUnsigned(), bytes[1].toUnsigned()))
                    .joinToString(TransferTicketConstant.OCTET_SEPARATOR)
            val port = (bytes[2].toUnsigned() shl TransferTicketConstant.BITS_PER_BYTE) or bytes[3].toUnsigned()
            if (port !in TransferTicketConstant.PORT_RANGE) return null
            return TransferTicket(host, port, bytes.copyOfRange(TransferTicketConstant.SHORT_CODE_HEADER_BYTES, bytes.size), Long.MAX_VALUE)
        }
    }
}

private fun Byte.toUnsigned(): Int = toInt() and TransferTicketConstant.BYTE_MASK

internal object TransferTicketConstant {
    const val PREFIX = "mwlxfer1"
    const val SEPARATOR = ";"
    const val PART_COUNT = 5
    const val SECRET_BYTES = 8

    /** Two IPv4 octets + a two-byte port. */
    const val SHORT_CODE_HEADER_BYTES = 4
    const val SHORT_CODE_BYTES = SHORT_CODE_HEADER_BYTES + SECRET_BYTES
    const val SHORT_CODE_GROUP = 4
    const val SHORT_CODE_GROUP_SEPARATOR = "-"
    const val OCTET_SEPARATOR = "."
    const val BITS_PER_BYTE = 8
    const val BYTE_MASK = 0xFF
    val PORT_RANGE = 1..65535
    val IPV4 = Regex("""^((25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(25[0-5]|2[0-4]\d|1?\d?\d)$""")
}

/**
 * Crockford's Base32 (https://www.crockford.com/base32.html): no I, L, O or U, so a typed code
 * can't be misread. Kotlin's stdlib has only Base64, hence this small codec.
 */
internal object CrockfordBase32 {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val BITS_PER_CHAR = 5
    private const val BITS_PER_BYTE = 8
    private const val CHAR_MASK = 0x1F
    private const val BYTE_MASK = 0xFF
    private val IGNORED = setOf('-', ' ')
    private val ALIASES = mapOf('O' to '0', 'I' to '1', 'L' to '1')

    fun encode(bytes: ByteArray): String =
        buildString {
            var buffer = 0
            var bits = 0
            for (byte in bytes) {
                buffer = (buffer shl BITS_PER_BYTE) or (byte.toInt() and BYTE_MASK)
                bits += BITS_PER_BYTE
                while (bits >= BITS_PER_CHAR) {
                    bits -= BITS_PER_CHAR
                    append(ALPHABET[(buffer shr bits) and CHAR_MASK])
                }
            }
            if (bits > 0) append(ALPHABET[(buffer shl (BITS_PER_CHAR - bits)) and CHAR_MASK])
        }

    /** Null if [text] has a character outside the alphabet (after normalising). Trailing pad bits
     *  that don't fill a byte are dropped. */
    fun decode(text: String): ByteArray? {
        val output = mutableListOf<Byte>()
        var buffer = 0
        var bits = 0
        for (raw in text.uppercase()) {
            if (raw in IGNORED) continue
            val value = ALPHABET.indexOf(ALIASES[raw] ?: raw)
            if (value < 0) return null
            buffer = (buffer shl BITS_PER_CHAR) or value
            bits += BITS_PER_CHAR
            if (bits >= BITS_PER_BYTE) {
                bits -= BITS_PER_BYTE
                output += (buffer shr bits).toByte()
            }
        }
        return output.toByteArray()
    }
}
