@file:OptIn(ExperimentalForeignApi::class)

package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.darwin.inet_ntop
import platform.darwin.inet_pton
import platform.posix.AF_INET
import platform.posix.EINPROGRESS
import platform.posix.EINTR
import platform.posix.F_GETFL
import platform.posix.F_SETFL
import platform.posix.INADDR_ANY
import platform.posix.INET_ADDRSTRLEN
import platform.posix.O_NONBLOCK
import platform.posix.POLLIN
import platform.posix.POLLOUT
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_ERROR
import platform.posix.SO_NOSIGPIPE
import platform.posix.SO_REUSEADDR
import platform.posix.accept
import platform.posix.bind
import platform.posix.close
import platform.posix.connect
import platform.posix.errno
import platform.posix.fcntl
import platform.posix.getsockname
import platform.posix.getsockopt
import platform.posix.listen
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.recv
import platform.posix.send
import platform.posix.setsockopt
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.socklen_tVar
import kotlin.coroutines.coroutineContext
import kotlin.time.TimeSource

private object IosTransferSocketConstant {
    /** accept()/connect() poll this often so cancellation is noticed promptly. */
    const val POLL_SLICE_MILLIS = 500
    const val LISTEN_BACKLOG = 4
    const val WIFI_INTERFACE = "en0"
    const val LOOPBACK_INTERFACE = "lo0"
    const val LINK_LOCAL_PREFIX = "169.254."
    const val BYTE_MASK = 0xFF
    const val BITS_PER_BYTE = 8
}

/** Host to network byte order for a port - Darwin's htons() is a macro, so cinterop has no function for it. */
private fun Int.toNetworkPort(): UShort =
    (
        ((this and IosTransferSocketConstant.BYTE_MASK) shl IosTransferSocketConstant.BITS_PER_BYTE) or
            ((this shr IosTransferSocketConstant.BITS_PER_BYTE) and IosTransferSocketConstant.BYTE_MASK)
    ).toUShort()

private fun UShort.fromNetworkPort(): Int = toInt().toNetworkPort().toInt()

private fun failure(operation: String): Nothing = throw TransferIoException("$operation failed (errno $errno)")

private fun Int.noSigPipe(): Int {
    memScoped {
        val on = alloc<IntVar>().apply { value = 1 }
        setsockopt(this@noSigPipe, SOL_SOCKET, SO_NOSIGPIPE, on.ptr, sizeOf<IntVar>().convert())
    }
    return this
}

/** poll() for [events] on [fd]; true when ready, false on timeout. */
private fun pollReady(
    fd: Int,
    events: Int,
    timeoutMillis: Int,
): Boolean =
    memScoped {
        val request = alloc<pollfd>()
        request.fd = fd
        request.events = events.toShort()
        val ready = poll(request.ptr, 1u, timeoutMillis)
        if (ready < 0 && errno != EINTR) failure("poll")
        ready > 0
    }

private class IosTransferConnection(
    private val fd: Int,
) : TransferConnection {
    override suspend fun readFully(
        byteCount: Int,
        timeoutMillis: Long,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(byteCount)
            val deadline = TimeSource.Monotonic.markNow()
            var offset = 0
            while (offset < byteCount) {
                coroutineContext.ensureActive()
                val remaining = timeoutMillis - deadline.elapsedNow().inWholeMilliseconds
                if (remaining <= 0) throw TransferIoException("read timed out")
                if (!pollReady(fd, POLLIN, minOf(remaining, IosTransferSocketConstant.POLL_SLICE_MILLIS.toLong()).toInt())) continue
                val read = buffer.usePinned { recv(fd, it.addressOf(offset), (byteCount - offset).convert(), 0) }
                if (read == 0L) throw TransferIoException("connection closed")
                if (read < 0) failure("recv")
                offset += read.toInt()
            }
            buffer
        }

    override suspend fun write(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            var offset = 0
            while (offset < bytes.size) {
                val sent = bytes.usePinned { send(fd, it.addressOf(offset), (bytes.size - offset).convert(), 0) }
                if (sent < 0) failure("send")
                offset += sent.toInt()
            }
        }
    }

    override fun close() {
        close(fd)
    }
}

private class IosTransferServer(
    private val fd: Int,
    override val port: Int,
) : TransferServer {
    override suspend fun accept(): TransferConnection =
        withContext(Dispatchers.IO) {
            var client = -1
            while (client < 0) {
                coroutineContext.ensureActive()
                try {
                    if (pollReady(fd, POLLIN, IosTransferSocketConstant.POLL_SLICE_MILLIS)) {
                        client = accept(fd, null, null)
                        if (client < 0 && errno != EINTR) failure("accept")
                    }
                } catch (e: TransferIoException) {
                    // Closing the server is how a cancelled offer stops accept(); that must end as
                    // a cancellation, not an error that escapes the cancelled coroutine.
                    coroutineContext.ensureActive()
                    throw e
                }
            }
            IosTransferConnection(client.noSigPipe())
        }

    override fun close() {
        close(fd)
    }
}

private object IosTransferSockets : TransferSockets {
    override suspend fun listen(): TransferServer =
        withContext(Dispatchers.IO) {
            val fd = socket(AF_INET, SOCK_STREAM, 0)
            if (fd < 0) failure("socket")
            memScoped {
                val on = alloc<IntVar>().apply { value = 1 }
                setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, on.ptr, sizeOf<IntVar>().convert())
                val address = alloc<sockaddr_in>()
                address.sin_len = sizeOf<sockaddr_in>().convert()
                address.sin_family = AF_INET.convert()
                address.sin_port = 0.toNetworkPort()
                address.sin_addr.s_addr = INADDR_ANY
                if (bind(fd, address.ptr.reinterpret<sockaddr>(), sizeOf<sockaddr_in>().convert()) != 0) {
                    close(fd)
                    failure("bind")
                }
                if (listen(fd, IosTransferSocketConstant.LISTEN_BACKLOG) != 0) {
                    close(fd)
                    failure("listen")
                }
                val bound = alloc<sockaddr_in>()
                val length = alloc<socklen_tVar>().apply { value = sizeOf<sockaddr_in>().convert() }
                getsockname(fd, bound.ptr.reinterpret<sockaddr>(), length.ptr)
                IosTransferServer(fd, bound.sin_port.fromNetworkPort())
            }
        }

    override suspend fun connect(
        host: String,
        port: Int,
        timeoutMillis: Long,
    ): TransferConnection =
        withContext(Dispatchers.IO) {
            val fd = socket(AF_INET, SOCK_STREAM, 0)
            if (fd < 0) failure("socket")
            try {
                memScoped {
                    val address = alloc<sockaddr_in>()
                    address.sin_len = sizeOf<sockaddr_in>().convert()
                    address.sin_family = AF_INET.convert()
                    address.sin_port = port.toNetworkPort()
                    if (inet_pton(AF_INET, host, address.sin_addr.ptr) != 1) throw TransferIoException("bad address")
                    // Non-blocking connect + poll, so an unreachable host fails after timeoutMillis
                    // instead of the OS's ~75 s default.
                    val flags = fcntl(fd, F_GETFL, 0)
                    fcntl(fd, F_SETFL, flags or O_NONBLOCK)
                    val result = connect(fd, address.ptr.reinterpret<sockaddr>(), sizeOf<sockaddr_in>().convert())
                    if (result != 0 && errno != EINPROGRESS) failure("connect")
                    if (result != 0) {
                        if (!pollReady(fd, POLLOUT, timeoutMillis.toInt())) throw TransferIoException("connect timed out")
                        val socketError = alloc<IntVar>()
                        val length = alloc<socklen_tVar>().apply { value = sizeOf<IntVar>().convert() }
                        getsockopt(fd, SOL_SOCKET, SO_ERROR, socketError.ptr, length.ptr)
                        if (socketError.value != 0) throw TransferIoException("connect failed (${socketError.value})")
                    }
                    fcntl(fd, F_SETFL, flags)
                }
                IosTransferConnection(fd.noSigPipe())
            } catch (e: TransferIoException) {
                close(fd)
                throw e
            }
        }

    override fun localIpv4Address(): String? =
        memScoped {
            val list = alloc<CPointerVar<ifaddrs>>()
            if (getifaddrs(list.ptr) != 0) return@memScoped null
            val candidates = mutableListOf<Pair<String, String>>()
            try {
                var current = list.value
                while (current != null) {
                    val entry = current.pointed
                    val address = entry.ifa_addr
                    if (address != null && address.pointed.sa_family.toInt() == AF_INET) {
                        val text = allocArray<ByteVar>(INET_ADDRSTRLEN)
                        inet_ntop(
                            AF_INET,
                            address
                                .reinterpret<sockaddr_in>()
                                .pointed.sin_addr.ptr,
                            text,
                            INET_ADDRSTRLEN.convert(),
                        )
                        val name = entry.ifa_name?.toKString().orEmpty()
                        val ip = text.toKString()
                        if (name != IosTransferSocketConstant.LOOPBACK_INTERFACE &&
                            !ip.startsWith(IosTransferSocketConstant.LINK_LOCAL_PREFIX)
                        ) {
                            candidates += name to ip
                        }
                    }
                    current = entry.ifa_next
                }
            } finally {
                freeifaddrs(list.value)
            }
            (candidates.firstOrNull { it.first == IosTransferSocketConstant.WIFI_INTERFACE } ?: candidates.firstOrNull())?.second
        }
}

actual fun platformTransferSockets(): TransferSockets = IosTransferSockets
