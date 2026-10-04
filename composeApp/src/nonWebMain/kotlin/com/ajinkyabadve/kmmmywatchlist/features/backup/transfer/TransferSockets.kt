package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

/** Any socket failure in the transfer - refused, timed out, reset, closed mid-message. Platform
 *  socket errors are converted to this so the protocol code catches one specific type. */
class TransferIoException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

interface TransferConnection {
    /** Reads exactly [byteCount] bytes, waiting at most [timeoutMillis] overall. */
    suspend fun readFully(
        byteCount: Int,
        timeoutMillis: Long,
    ): ByteArray

    suspend fun write(bytes: ByteArray)

    fun close()
}

interface TransferServer {
    val port: Int

    /** Waits for the next incoming connection; cancellable. */
    suspend fun accept(): TransferConnection

    fun close()
}

/**
 * The transfer's plain TCP sockets. Platform equivalent checked first, per the code conventions:
 * ktor-network would cover every target, but this app's Ktor is 2.3.x, whose iOS ktor-network klib
 * fails to link with Kotlin 2.3 (`Failed to build cache ... Expected a type operator call`,
 * confirmed 2026-10-04), and moving the whole app to Ktor 3 for one socket isn't worth it. So:
 * `java.net` on Android/desktop, POSIX sockets on iOS.
 */
interface TransferSockets {
    /** Listens on every interface, on a port the OS picks. */
    suspend fun listen(): TransferServer

    suspend fun connect(
        host: String,
        port: Int,
        timeoutMillis: Long,
    ): TransferConnection

    /** This device's IPv4 address on the local network (Wi-Fi first), or null when it has none. */
    fun localIpv4Address(): String?
}

expect fun platformTransferSockets(): TransferSockets

/** Shown on the sending device's "Send your backup to …?" prompt. */
expect fun transferDeviceName(): String
