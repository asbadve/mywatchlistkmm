package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.BackupRepository
import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.BackupRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.backup.transfer.DeviceTransfer
import com.ajinkyabadve.kmmmywatchlist.features.backup.transfer.IncomingTransferRequest
import com.ajinkyabadve.kmmmywatchlist.features.backup.transfer.TransferIoException
import com.ajinkyabadve.kmmmywatchlist.features.backup.transfer.TransferOffer
import com.ajinkyabadve.kmmmywatchlist.features.backup.transfer.TransferOfferResult
import com.ajinkyabadve.kmmmywatchlist.features.backup.transfer.TransferReceiveResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SendTransferState {
    data object Preparing : SendTransferState

    /** [code] is drawn as the QR code; [shortCode] is the same ticket for typing. */
    data class ShowingCode(
        val code: String,
        val shortCode: String,
    ) : SendTransferState

    data class Request(
        val deviceName: String,
    ) : SendTransferState

    data object Sending : SendTransferState

    data class Sent(
        val deviceName: String,
    ) : SendTransferState

    data object Declined : SendTransferState

    data object Expired : SendTransferState

    data object NoNetwork : SendTransferState

    data object Failed : SendTransferState
}

sealed interface ReceiveTransferState {
    data object Scanning : ReceiveTransferState

    /** Connected (or connecting) and waiting for the sender to tap Send. */
    data object Waiting : ReceiveTransferState

    data class Received(
        val backupJson: String,
    ) : ReceiveTransferState

    data object InvalidCode : ReceiveTransferState

    data object Expired : ReceiveTransferState

    data object Unreachable : ReceiveTransferState

    data object Declined : ReceiveTransferState

    data object Failed : ReceiveTransferState
}

/**
 * Item 15 stage 2's two flows. Sending: show a QR code, wait for a receiver that holds its key,
 * ask before sending. Receiving: scan, connect, wait for the sender's approval, then hand the
 * backup JSON to stage 1's confirm-and-restore (the receiving person still confirms the restore).
 */
class DeviceTransferScreenModel(
    private val deviceTransfer: DeviceTransfer = DeviceTransfer(),
    private val backupRepository: BackupRepository = BackupRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null
    private var offer: TransferOffer? = null
    private var request: IncomingTransferRequest? = null

    private val _sendState = MutableStateFlow<SendTransferState>(SendTransferState.Preparing)
    val sendState: StateFlow<SendTransferState> = _sendState.asStateFlow()

    private val _receiveState = MutableStateFlow<ReceiveTransferState>(ReceiveTransferState.Scanning)
    val receiveState: StateFlow<ReceiveTransferState> = _receiveState.asStateFlow()

    fun startSending() {
        stop()
        _sendState.value = SendTransferState.Preparing
        job =
            viewModelScope.launch {
                when (val result = deviceTransfer.createOffer()) {
                    TransferOfferResult.NoNetwork -> _sendState.value = SendTransferState.NoNetwork
                    TransferOfferResult.Failed -> _sendState.value = SendTransferState.Failed
                    is TransferOfferResult.Ready -> {
                        offer = result.offer
                        _sendState.value = SendTransferState.ShowingCode(result.offer.ticket.encode(), result.offer.ticket.toShortCode())
                        val incoming =
                            try {
                                result.offer.awaitRequest()
                            } catch (e: TransferIoException) {
                                result.offer.close()
                                _sendState.value = SendTransferState.Failed
                                return@launch
                            }
                        // One transfer per code: stop listening as soon as someone legitimate shows up.
                        result.offer.close()
                        if (incoming == null) {
                            _sendState.value = SendTransferState.Expired
                        } else {
                            request = incoming
                            _sendState.value = SendTransferState.Request(incoming.deviceName)
                        }
                    }
                }
            }
    }

    fun approve() {
        val incoming = request ?: return
        request = null
        _sendState.value = SendTransferState.Sending
        job =
            viewModelScope.launch {
                _sendState.value =
                    try {
                        incoming.approve(backupRepository.exportToJson())
                        SendTransferState.Sent(incoming.deviceName)
                    } catch (e: TransferIoException) {
                        SendTransferState.Failed
                    }
            }
    }

    fun decline() {
        val incoming = request ?: return
        request = null
        _sendState.value = SendTransferState.Declined
        job = viewModelScope.launch { incoming.decline() }
    }

    fun startReceiving() {
        stop()
        _receiveState.value = ReceiveTransferState.Scanning
    }

    /** A scanned QR code or a typed short code. The scanner can report the same code many times a
     *  second; only the first one counts. */
    fun onCodeScanned(text: String) {
        if (_receiveState.value != ReceiveTransferState.Scanning) return
        _receiveState.value = ReceiveTransferState.Waiting
        job =
            viewModelScope.launch {
                _receiveState.value =
                    when (val result = deviceTransfer.receive(text)) {
                        is TransferReceiveResult.Received -> ReceiveTransferState.Received(result.backupJson)
                        TransferReceiveResult.InvalidCode -> ReceiveTransferState.InvalidCode
                        TransferReceiveResult.Expired -> ReceiveTransferState.Expired
                        TransferReceiveResult.Unreachable -> ReceiveTransferState.Unreachable
                        TransferReceiveResult.Declined -> ReceiveTransferState.Declined
                        TransferReceiveResult.Failed -> ReceiveTransferState.Failed
                    }
            }
    }

    /** Cancels whatever is in flight and stops listening - called when the dialog closes. */
    fun stop() {
        job?.cancel()
        job = null
        offer?.close()
        offer = null
        // A receiver left waiting at the prompt is told no, rather than timing out after minutes.
        request?.let { pending -> viewModelScope.launch { pending.decline() } }
        request = null
    }

    override fun onCleared() {
        stop()
        viewModelScope.cancel()
    }
}
