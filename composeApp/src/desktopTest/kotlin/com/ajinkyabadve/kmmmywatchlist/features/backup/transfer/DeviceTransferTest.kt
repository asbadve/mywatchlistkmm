package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Real sockets on loopback - the platform implementation, with the advertised address pinned to
 *  127.0.0.1 so the test doesn't depend on the machine having Wi-Fi. */
class DeviceTransferTest {
    private val loopbackSockets =
        object : TransferSockets by platformTransferSockets() {
            override fun localIpv4Address(): String = LOOPBACK
        }

    private fun transfer(now: Long = NOW) = DeviceTransfer(loopbackSockets, nowEpochSeconds = { now }, deviceName = { RECEIVER_NAME })

    private suspend fun ready(transfer: DeviceTransfer) = assertIs<TransferOfferResult.Ready>(transfer.createOffer()).offer

    @Test
    fun testCipher_roundTripsAndRejectsAWrongKey() =
        runTest {
            val cipher = TransferCipher.fromSecret(TransferCipher.generateSecret())
            val other = TransferCipher.fromSecret(TransferCipher.generateSecret())
            val encrypted = cipher.encrypt(PAYLOAD.encodeToByteArray(), TransferProtocolConstant.TO_SENDER)

            assertContentEquals(PAYLOAD.encodeToByteArray(), cipher.decryptOrNull(encrypted, TransferProtocolConstant.TO_SENDER))
            assertNull(other.decryptOrNull(encrypted, TransferProtocolConstant.TO_SENDER))
            // A frame can't be replayed in the other direction.
            assertNull(cipher.decryptOrNull(encrypted, TransferProtocolConstant.TO_RECEIVER))
        }

    @Test
    fun testApprovedTransfer_deliversTheBackupAndNamesTheReceiver() =
        runTest {
            val sender = transfer()
            val offer = ready(sender)
            withContext(Dispatchers.Default) {
                val received = async { transfer().receive(offer.ticket.encode()) }
                val request = assertIs<IncomingTransferRequest>(offer.awaitRequest())
                assertEquals(RECEIVER_NAME, request.deviceName)
                request.approve(PAYLOAD)

                assertEquals(TransferReceiveResult.Received(PAYLOAD), received.await())
            }
            offer.close()
        }

    @Test
    fun testATypedShortCode_transfersLikeAScannedOne() =
        runTest {
            val offer = ready(transfer())
            withContext(Dispatchers.Default) {
                val received = async { transfer().receive(offer.ticket.toShortCode()) }
                assertIs<IncomingTransferRequest>(offer.awaitRequest()).approve(PAYLOAD)

                assertEquals(TransferReceiveResult.Received(PAYLOAD), received.await())
            }
            offer.close()
        }

    @Test
    fun testDeclinedTransfer_tellsTheReceiver() =
        runTest {
            val offer = ready(transfer())
            withContext(Dispatchers.Default) {
                val received = async { transfer().receive(offer.ticket.encode()) }
                assertIs<IncomingTransferRequest>(offer.awaitRequest()).decline()

                assertEquals(TransferReceiveResult.Declined, received.await())
            }
            offer.close()
        }

    @Test
    fun testAConnectionWithTheWrongKey_neverReachesThePrompt() =
        runTest {
            val offer = ready(transfer())
            val forged =
                TransferTicket(offer.ticket.host, offer.ticket.port, TransferCipher.generateSecret(), offer.ticket.expiresAtEpochSeconds)
            withContext(Dispatchers.Default) {
                val intruder = async { transfer().receive(forged.encode()) }
                val request = async { offer.awaitRequest() }
                // The intruder is dropped; the real receiver is the one the sender gets asked about.
                assertEquals(TransferReceiveResult.Failed, intruder.await())
                val genuine = async { DeviceTransfer(loopbackSockets, { NOW }, { GENUINE_NAME }).receive(offer.ticket.encode()) }
                val incoming = assertIs<IncomingTransferRequest>(request.await())
                assertEquals(GENUINE_NAME, incoming.deviceName)
                incoming.approve(PAYLOAD)
                assertEquals(TransferReceiveResult.Received(PAYLOAD), genuine.await())
            }
            offer.close()
        }

    @Test
    fun testReceive_refusesExpiredInvalidAndUnreachableCodes() =
        runTest {
            val offer = ready(transfer())
            val code = offer.ticket.encode()
            offer.close()

            assertEquals(TransferReceiveResult.Expired, transfer(now = offer.ticket.expiresAtEpochSeconds).receive(code))
            assertEquals(TransferReceiveResult.InvalidCode, transfer().receive(NOT_A_CODE))
            withContext(Dispatchers.Default) {
                assertEquals(TransferReceiveResult.Unreachable, transfer().receive(code))
            }
        }

    @Test
    fun testClosingAWaitingOffer_endsAsACancellationNotAnError() =
        runTest {
            val offer = ready(transfer())
            withContext(Dispatchers.Default) {
                supervisorScope {
                    val waiting = async { offer.awaitRequest() }
                    delay(WAIT_BEFORE_CLOSE_MILLIS)

                    // Same order as DeviceTransferScreenModel.stop(): cancel, then close the socket.
                    waiting.cancel()
                    offer.close()
                    waiting.join()

                    assertIs<CancellationException>(waiting.getCompletionExceptionOrNull())
                }
            }
        }

    @Test
    fun testCreateOffer_withoutANetworkAddressSaysSo() =
        runTest {
            val offline =
                object : TransferSockets by platformTransferSockets() {
                    override fun localIpv4Address(): String? = null
                }

            assertEquals(TransferOfferResult.NoNetwork, DeviceTransfer(offline, { NOW }, { RECEIVER_NAME }).createOffer())
        }

    private companion object {
        const val LOOPBACK = "127.0.0.1"
        const val NOW = 1_791_000_000L
        const val RECEIVER_NAME = "Pixel 10"
        const val GENUINE_NAME = "iPhone 17"
        const val PAYLOAD = """{"format":1,"exportedAt":0}"""
        const val NOT_A_CODE = "https://example.com"
        const val WAIT_BEFORE_CLOSE_MILLIS = 300L
    }
}
