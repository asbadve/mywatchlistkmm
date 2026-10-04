package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import com.ajinkyabadve.kmmmywatchlist.features.backup.transfer.DeviceTransfer
import com.ajinkyabadve.kmmmywatchlist.features.backup.transfer.TransferSockets
import com.ajinkyabadve.kmmmywatchlist.features.backup.transfer.platformTransferSockets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Two real screen models talking over loopback sockets - the whole stage-2 flow minus camera and UI. */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceTransferScreenModelTest {
    private val loopback =
        object : TransferSockets by platformTransferSockets() {
            override fun localIpv4Address(): String = LOOPBACK
        }

    @BeforeTest
    fun setUp() {
        // Real socket I/O, so a real dispatcher rather than a test one.
        Dispatchers.setMain(Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun model(
        deviceName: String,
        fixture: BackupTestFixture = BackupTestFixture(),
    ) = DeviceTransferScreenModel(DeviceTransfer(loopback, deviceName = { deviceName }), fixture.repository)

    private suspend fun <T> StateFlow<T>.awaitState(predicate: (T) -> Boolean): T = withTimeout(TIMEOUT_MILLIS) { first(predicate) }

    @Test
    fun testSendAndReceive_endToEnd() =
        runBlocking {
            val oldPhone = BackupTestFixture().apply { people.seedFavorite(BackupTestConstant.PERSON_ID, BackupTestConstant.PERSON_NAME) }
            val sender = model(SENDER_NAME, oldPhone)
            val receiver = model(RECEIVER_NAME)

            sender.startSending()
            val code = assertIs<SendTransferState.ShowingCode>(sender.sendState.awaitState { it is SendTransferState.ShowingCode }).code
            receiver.startReceiving()
            receiver.onCodeScanned(code)
            // The scanner fires repeatedly; extra scans must not start a second connection.
            receiver.onCodeScanned(code)

            val request = assertIs<SendTransferState.Request>(sender.sendState.awaitState { it is SendTransferState.Request })
            assertEquals(RECEIVER_NAME, request.deviceName)
            sender.approve()

            val received = assertIs<ReceiveTransferState.Received>(receiver.receiveState.awaitState { it is ReceiveTransferState.Received })
            assertTrue(received.backupJson.contains(BackupTestConstant.PERSON_NAME))
            assertEquals(SendTransferState.Sent(RECEIVER_NAME), sender.sendState.awaitState { it is SendTransferState.Sent })
            sender.stop()
        }

    @Test
    fun testTypingTheShortCode_worksLikeScanning() =
        runBlocking {
            val sender = model(SENDER_NAME)
            val receiver = model(RECEIVER_NAME)
            sender.startSending()
            val showing = assertIs<SendTransferState.ShowingCode>(sender.sendState.awaitState { it is SendTransferState.ShowingCode })

            receiver.onCodeScanned(showing.shortCode)
            sender.sendState.awaitState { it is SendTransferState.Request }
            sender.approve()

            assertIs<ReceiveTransferState.Received>(receiver.receiveState.awaitState { it is ReceiveTransferState.Received })
            sender.stop()
        }

    @Test
    fun testDecline_reachesTheReceiver() =
        runBlocking {
            val sender = model(SENDER_NAME)
            val receiver = model(RECEIVER_NAME)
            sender.startSending()
            val code = assertIs<SendTransferState.ShowingCode>(sender.sendState.awaitState { it is SendTransferState.ShowingCode }).code
            receiver.onCodeScanned(code)
            sender.sendState.awaitState { it is SendTransferState.Request }

            sender.decline()

            assertEquals(ReceiveTransferState.Declined, receiver.receiveState.awaitState { it == ReceiveTransferState.Declined })
            sender.stop()
        }

    @Test
    fun testScanningSomethingElse_saysItIsNotATransferCode() =
        runBlocking {
            val receiver = model(RECEIVER_NAME)

            receiver.onCodeScanned(NOT_A_CODE)

            assertEquals(ReceiveTransferState.InvalidCode, receiver.receiveState.awaitState { it == ReceiveTransferState.InvalidCode })
        }

    private companion object {
        const val LOOPBACK = "127.0.0.1"
        const val SENDER_NAME = "Pixel 10"
        const val RECEIVER_NAME = "iPhone 17"
        const val NOT_A_CODE = "https://example.com"
        const val TIMEOUT_MILLIS = 15_000L
    }
}
