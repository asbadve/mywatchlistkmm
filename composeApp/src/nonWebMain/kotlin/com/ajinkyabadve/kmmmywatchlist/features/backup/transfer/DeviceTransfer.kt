package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock

object DeviceTransferConstant {
    /** How long a QR code stays valid - long enough to walk the other phone over, short enough
     *  that a photo of it is useless later. */
    const val OFFER_LIFETIME_SECONDS = 5 * 60L
    const val MILLIS_PER_SECOND = 1_000L
    const val CONNECT_TIMEOUT_MILLIS = 10_000L

    /** How long the receiver's hello may take to arrive once a device has connected. */
    const val HELLO_TIMEOUT_MILLIS = 10_000L

    /** How long the receiver waits for the person on the sending phone to tap Send. */
    const val APPROVAL_TIMEOUT_MILLIS = 2 * 60_000L
}

/** A connected receiver that has proved it holds the key. The sender's UI shows [deviceName] and
 *  calls [approve] or [decline] - exactly once. */
class IncomingTransferRequest internal constructor(
    val deviceName: String,
    private val connection: TransferConnection,
    private val cipher: TransferCipher,
) {
    suspend fun approve(backupJson: String) {
        try {
            connection.writeMessage(
                cipher,
                TransferMessage(type = TransferProtocolConstant.TYPE_BACKUP, backup = backupJson),
                TransferProtocolConstant.TO_RECEIVER,
            )
        } finally {
            connection.close()
        }
    }

    suspend fun decline() {
        try {
            connection.writeMessage(
                cipher,
                TransferMessage(type = TransferProtocolConstant.TYPE_DECLINED),
                TransferProtocolConstant.TO_RECEIVER,
            )
        } catch (e: TransferIoException) {
            // The receiver already gave up; nothing to tell it.
        } finally {
            connection.close()
        }
    }
}

/**
 * The sending side of one offer: a listening socket and the ticket that points at it. Closing it
 * (or the offer expiring) stops accepting connections.
 */
class TransferOffer internal constructor(
    val ticket: TransferTicket,
    private val server: TransferServer,
    private val cipher: TransferCipher,
    private val nowEpochSeconds: () -> Long,
) {
    /**
     * Waits for a receiver whose hello decrypts with this offer's key. Anything else that connects
     * - a port scanner, a stale code, garbage - is dropped silently and the wait goes on, so only
     * a real receiver ever reaches the "Send?" prompt. Null once the offer expires; throws
     * [TransferIoException] if listening itself fails, and is cancelled (not failed) by [close].
     */
    suspend fun awaitRequest(): IncomingTransferRequest? {
        val remainingMillis = (ticket.expiresAtEpochSeconds - nowEpochSeconds()) * DeviceTransferConstant.MILLIS_PER_SECOND
        if (remainingMillis <= 0) return null
        return withTimeoutOrNull(remainingMillis) {
            var request: IncomingTransferRequest? = null
            while (request == null) {
                val connection = server.accept()
                val hello =
                    try {
                        connection.readMessage(cipher, TransferProtocolConstant.TO_SENDER, DeviceTransferConstant.HELLO_TIMEOUT_MILLIS)
                    } catch (e: TransferIoException) {
                        null
                    }
                if (hello?.type == TransferProtocolConstant.TYPE_HELLO) {
                    request = IncomingTransferRequest(hello.deviceName.orEmpty(), connection, cipher)
                } else {
                    connection.close()
                }
            }
            request
        }
    }

    fun close() {
        server.close()
    }
}

sealed interface TransferOfferResult {
    data class Ready(
        val offer: TransferOffer,
    ) : TransferOfferResult

    /** No local network address - not on Wi-Fi (or only on cellular). */
    data object NoNetwork : TransferOfferResult

    data object Failed : TransferOfferResult
}

sealed interface TransferReceiveResult {
    data class Received(
        val backupJson: String,
    ) : TransferReceiveResult

    data object Declined : TransferReceiveResult

    /** The scanned QR code isn't a MyWatchList transfer code. */
    data object InvalidCode : TransferReceiveResult

    data object Expired : TransferReceiveResult

    /** Couldn't connect - usually not on the same Wi-Fi, or a guest network isolating devices. */
    data object Unreachable : TransferReceiveResult

    /** Connected, but the sender didn't answer in time or the conversation broke off. */
    data object Failed : TransferReceiveResult
}

/**
 * Both ends of item 15 stage 2's device-to-device transfer. The sender shows a QR code
 * ([TransferTicket]); the receiver scans it, connects over the local network, and gets the same
 * backup JSON a file export would hold - which then goes through stage 1's confirm-and-restore.
 */
class DeviceTransfer(
    private val sockets: TransferSockets = platformTransferSockets(),
    private val nowEpochSeconds: () -> Long = { Clock.System.now().epochSeconds },
    private val deviceName: () -> String = { transferDeviceName() },
) {
    suspend fun createOffer(): TransferOfferResult {
        val host = sockets.localIpv4Address() ?: return TransferOfferResult.NoNetwork
        return try {
            val secret = TransferCipher.generateSecret()
            val server = sockets.listen()
            val ticket = TransferTicket(host, server.port, secret, nowEpochSeconds() + DeviceTransferConstant.OFFER_LIFETIME_SECONDS)
            TransferOfferResult.Ready(TransferOffer(ticket, server, TransferCipher.fromSecret(secret), nowEpochSeconds))
        } catch (e: TransferIoException) {
            TransferOfferResult.Failed
        }
    }

    /** [codeText] is either a scanned QR code or a typed short code. */
    suspend fun receive(codeText: String): TransferReceiveResult {
        val ticket =
            TransferTicket.decode(codeText)
                ?: shortCodeTicket(codeText)
                ?: return TransferReceiveResult.InvalidCode
        if (ticket.isExpired(nowEpochSeconds())) return TransferReceiveResult.Expired
        val cipher = TransferCipher.fromSecret(ticket.secret)
        val connection =
            try {
                sockets.connect(ticket.host, ticket.port, DeviceTransferConstant.CONNECT_TIMEOUT_MILLIS)
            } catch (e: TransferIoException) {
                return TransferReceiveResult.Unreachable
            }
        return try {
            connection.writeMessage(
                cipher,
                TransferMessage(type = TransferProtocolConstant.TYPE_HELLO, deviceName = deviceName()),
                TransferProtocolConstant.TO_SENDER,
            )
            val reply = connection.readMessage(cipher, TransferProtocolConstant.TO_RECEIVER, DeviceTransferConstant.APPROVAL_TIMEOUT_MILLIS)
            when {
                reply?.type == TransferProtocolConstant.TYPE_BACKUP && reply.backup != null -> TransferReceiveResult.Received(reply.backup)
                reply?.type == TransferProtocolConstant.TYPE_DECLINED -> TransferReceiveResult.Declined
                else -> TransferReceiveResult.Failed
            }
        } catch (e: TransferIoException) {
            TransferReceiveResult.Failed
        } finally {
            connection.close()
        }
    }

    private fun shortCodeTicket(codeText: String): TransferTicket? =
        sockets.localIpv4Address()?.let { receiverIp -> TransferTicket.fromShortCode(codeText, receiverIp) }
}
