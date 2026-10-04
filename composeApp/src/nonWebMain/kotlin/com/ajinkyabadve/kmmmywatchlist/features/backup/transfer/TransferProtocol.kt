package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The whole conversation is two encrypted messages over one TCP connection:
 * 1. receiver -> sender: `hello`, carrying its device name. Only a device holding the ticket's key
 *    can produce one that decrypts, so this is also the receiver proving it scanned the code.
 * 2. sender -> receiver: `backup` (the stage-1 envelope JSON) or `declined`.
 *
 * Each message is framed as a 4-byte big-endian length, then the ciphertext.
 */
@Serializable
internal data class TransferMessage(
    val type: String,
    val deviceName: String? = null,
    val backup: String? = null,
)

internal object TransferProtocolConstant {
    const val TYPE_HELLO = "hello"
    const val TYPE_BACKUP = "backup"
    const val TYPE_DECLINED = "declined"
    val TO_SENDER = "mywatchlist-transfer-v1:to-sender".encodeToByteArray()
    val TO_RECEIVER = "mywatchlist-transfer-v1:to-receiver".encodeToByteArray()
    const val LENGTH_BYTES = 4

    /** A backup is a few KB; anything this large is not one of ours. */
    const val MAX_FRAME_BYTES = 1 shl 20
    const val BITS_PER_BYTE = 8
    const val BYTE_MASK = 0xFF
}

private val transferJson = Json { ignoreUnknownKeys = true }

internal suspend fun TransferConnection.writeMessage(
    cipher: TransferCipher,
    message: TransferMessage,
    associatedData: ByteArray,
) {
    val frame = cipher.encrypt(transferJson.encodeToString(TransferMessage.serializer(), message).encodeToByteArray(), associatedData)
    write(frame.size.toLengthPrefix() + frame)
}

/** Null when the frame doesn't decrypt with this key or isn't a message - never an exception for
 *  bad *content*; socket failures still throw [TransferIoException]. */
internal suspend fun TransferConnection.readMessage(
    cipher: TransferCipher,
    associatedData: ByteArray,
    timeoutMillis: Long,
): TransferMessage? {
    val length = readFully(TransferProtocolConstant.LENGTH_BYTES, timeoutMillis).fromLengthPrefix()
    if (length <= 0 || length > TransferProtocolConstant.MAX_FRAME_BYTES) return null
    val plaintext = cipher.decryptOrNull(readFully(length, timeoutMillis), associatedData) ?: return null
    return try {
        transferJson.decodeFromString(TransferMessage.serializer(), plaintext.decodeToString())
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}

private fun Int.toLengthPrefix(): ByteArray =
    ByteArray(TransferProtocolConstant.LENGTH_BYTES) { index ->
        (this shr (TransferProtocolConstant.BITS_PER_BYTE * (TransferProtocolConstant.LENGTH_BYTES - 1 - index))).toByte()
    }

private fun ByteArray.fromLengthPrefix(): Int =
    fold(0) { acc, byte ->
        (acc shl TransferProtocolConstant.BITS_PER_BYTE) or
            (byte.toInt() and TransferProtocolConstant.BYTE_MASK)
    }
