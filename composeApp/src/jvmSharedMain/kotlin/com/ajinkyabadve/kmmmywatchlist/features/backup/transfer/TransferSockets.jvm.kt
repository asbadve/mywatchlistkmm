package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.coroutines.coroutineContext

private object JvmTransferSocketConstant {
    /** accept() wakes this often to notice cancellation - java.net accept isn't interruptible. */
    const val ACCEPT_POLL_MILLIS = 500

    /** Wi-Fi interface name prefixes, tried before any other site-local IPv4 address
     *  (Android: wlan0; macOS: en0/en1; Linux: wlp*, eth*). */
    val PREFERRED_INTERFACE_PREFIXES = listOf("wlan", "en", "wl", "eth")
}

private class JvmTransferConnection(
    private val socket: Socket,
) : TransferConnection {
    private val input = DataInputStream(socket.getInputStream())

    override suspend fun readFully(
        byteCount: Int,
        timeoutMillis: Long,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            try {
                socket.soTimeout = timeoutMillis.toInt()
                ByteArray(byteCount).also { input.readFully(it) }
            } catch (e: IOException) {
                throw TransferIoException("read failed", e)
            }
        }

    override suspend fun write(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                socket.getOutputStream().apply {
                    write(bytes)
                    flush()
                }
            } catch (e: IOException) {
                throw TransferIoException("write failed", e)
            }
        }
    }

    override fun close() {
        try {
            socket.close()
        } catch (e: IOException) {
            // Already gone.
        }
    }
}

private class JvmTransferServer(
    private val serverSocket: ServerSocket,
) : TransferServer {
    override val port: Int = serverSocket.localPort

    override suspend fun accept(): TransferConnection =
        withContext(Dispatchers.IO) {
            serverSocket.soTimeout = JvmTransferSocketConstant.ACCEPT_POLL_MILLIS
            var connection: TransferConnection? = null
            while (connection == null) {
                coroutineContext.ensureActive()
                connection =
                    try {
                        JvmTransferConnection(serverSocket.accept())
                    } catch (e: SocketTimeoutException) {
                        null
                    } catch (e: IOException) {
                        // Closing the server is how a cancelled offer stops accept(); that must end
                        // as a cancellation, not an error that escapes the cancelled coroutine.
                        coroutineContext.ensureActive()
                        throw TransferIoException("accept failed", e)
                    }
            }
            connection
        }

    override fun close() {
        try {
            serverSocket.close()
        } catch (e: IOException) {
            // Already closed.
        }
    }
}

private object JvmTransferSockets : TransferSockets {
    override suspend fun listen(): TransferServer =
        withContext(Dispatchers.IO) {
            try {
                JvmTransferServer(ServerSocket(0))
            } catch (e: IOException) {
                throw TransferIoException("listen failed", e)
            }
        }

    override suspend fun connect(
        host: String,
        port: Int,
        timeoutMillis: Long,
    ): TransferConnection =
        withContext(Dispatchers.IO) {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(host, port), timeoutMillis.toInt())
                JvmTransferConnection(socket)
            } catch (e: IOException) {
                socket.close()
                throw TransferIoException("connect failed", e)
            }
        }

    override fun localIpv4Address(): String? {
        val candidates =
            try {
                NetworkInterface
                    .getNetworkInterfaces()
                    ?.toList()
                    .orEmpty()
                    .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                    .flatMap { networkInterface ->
                        networkInterface.inetAddresses
                            .toList()
                            .filterIsInstance<Inet4Address>()
                            .filter { it.isSiteLocalAddress }
                            .map { networkInterface.name to it.hostAddress }
                    }
            } catch (e: SocketException) {
                emptyList()
            }
        val preferred =
            candidates.firstOrNull { (name, _) -> JvmTransferSocketConstant.PREFERRED_INTERFACE_PREFIXES.any { name.startsWith(it) } }
        return (preferred ?: candidates.firstOrNull())?.second
    }
}

actual fun platformTransferSockets(): TransferSockets = JvmTransferSockets
