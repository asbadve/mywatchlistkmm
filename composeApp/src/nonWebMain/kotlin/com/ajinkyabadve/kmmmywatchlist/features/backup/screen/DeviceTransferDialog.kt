package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.isMobilePlatform
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.transfer_camera_denied
import mywatchlist.composeapp.generated.resources.transfer_code_label
import mywatchlist.composeapp.generated.resources.transfer_code_validity
import mywatchlist.composeapp.generated.resources.transfer_connect
import mywatchlist.composeapp.generated.resources.transfer_declined_receiver
import mywatchlist.composeapp.generated.resources.transfer_declined_sender
import mywatchlist.composeapp.generated.resources.transfer_expired
import mywatchlist.composeapp.generated.resources.transfer_failed
import mywatchlist.composeapp.generated.resources.transfer_invalid_code
import mywatchlist.composeapp.generated.resources.transfer_no_network
import mywatchlist.composeapp.generated.resources.transfer_preparing
import mywatchlist.composeapp.generated.resources.transfer_qr_content_description
import mywatchlist.composeapp.generated.resources.transfer_receive_action
import mywatchlist.composeapp.generated.resources.transfer_request_decline
import mywatchlist.composeapp.generated.resources.transfer_request_message
import mywatchlist.composeapp.generated.resources.transfer_request_send
import mywatchlist.composeapp.generated.resources.transfer_request_unknown_device
import mywatchlist.composeapp.generated.resources.transfer_scan_again
import mywatchlist.composeapp.generated.resources.transfer_scan_instructions
import mywatchlist.composeapp.generated.resources.transfer_send_action
import mywatchlist.composeapp.generated.resources.transfer_send_instructions
import mywatchlist.composeapp.generated.resources.transfer_sending
import mywatchlist.composeapp.generated.resources.transfer_sent
import mywatchlist.composeapp.generated.resources.transfer_short_code_hint
import mywatchlist.composeapp.generated.resources.transfer_try_again
import mywatchlist.composeapp.generated.resources.transfer_type_instructions
import mywatchlist.composeapp.generated.resources.transfer_unreachable
import mywatchlist.composeapp.generated.resources.transfer_waiting
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private object DeviceTransferDialogConstant {
    val CONTENT_MAX_WIDTH = 480.dp
    val QR_SIZE = 260.dp
    val QR_QUIET_ZONE = 16.dp
    val QR_CORNER = 16.dp
    val SCANNER_CORNER = 24.dp
    val PROGRESS_SIZE = 48.dp
    val SHORT_CODE_MIN_FONT = 14.sp
}

actual fun supportsTransferSending(): Boolean = true

// Every non-web platform: phones scan or type the code, desktop types it.
actual fun supportsTransferReceiving(): Boolean = true

@Composable
actual fun DeviceTransferDialog(
    mode: DeviceTransferMode,
    onBackupReceived: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val screenModel = viewModel { DeviceTransferScreenModel() }
    DisposableEffect(mode) {
        when (mode) {
            DeviceTransferMode.SEND -> screenModel.startSending()
            DeviceTransferMode.RECEIVE -> screenModel.startReceiving()
        }
        onDispose { screenModel.stop() }
    }
    // Full screen rather than an AlertDialog: the QR code and the camera preview need the room.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        when (mode) {
            DeviceTransferMode.SEND -> {
                val state by screenModel.sendState.collectAsState()
                SendTransferContent(
                    state = state,
                    onApprove = screenModel::approve,
                    onDecline = screenModel::decline,
                    onRetry = screenModel::startSending,
                    onClose = onDismiss,
                )
            }

            DeviceTransferMode.RECEIVE -> {
                val state by screenModel.receiveState.collectAsState()
                LaunchedEffect(state) {
                    (state as? ReceiveTransferState.Received)?.let { onBackupReceived(it.backupJson) }
                }
                ReceiveTransferContent(
                    state = state,
                    scanner = {
                        TransferQrScanner(
                            onCodeScanned = screenModel::onCodeScanned,
                            permissionDenied = { TransferMessage(Res.string.transfer_camera_denied) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                    onRetry = screenModel::startReceiving,
                    onClose = onDismiss,
                    onCodeEntered = screenModel::onCodeScanned,
                )
            }
        }
    }
}

/** The sending side's screens - stateless, so each state can be UI-tested without sockets. */
@Composable
internal fun SendTransferContent(
    state: SendTransferState,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    TransferScaffold(title = stringResource(Res.string.transfer_send_action), onClose = onClose) {
        when (state) {
            SendTransferState.Preparing -> TransferProgress(Res.string.transfer_preparing)

            is SendTransferState.ShowingCode -> {
                // Always dark modules on white, whatever the theme: scanners need the contrast and
                // the quiet zone around the code.
                Image(
                    painter = rememberQrCodePainter(state.code),
                    contentDescription = stringResource(Res.string.transfer_qr_content_description),
                    modifier =
                        Modifier
                            .size(DeviceTransferDialogConstant.QR_SIZE)
                            .clip(RoundedCornerShape(DeviceTransferDialogConstant.QR_CORNER))
                            .background(Color.White)
                            .padding(DeviceTransferDialogConstant.QR_QUIET_ZONE),
                )
                TransferMessage(Res.string.transfer_send_instructions)
                Text(
                    text = stringResource(Res.string.transfer_short_code_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                // Selectable, so it can also be copied into a chat or email to the other device.
                SelectionContainer {
                    // One line, shrinking to fit: wrapping split the code mid-group on narrow phones.
                    BasicText(
                        text = state.shortCode,
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            ),
                        maxLines = 1,
                        autoSize =
                            TextAutoSize.StepBased(
                                minFontSize = DeviceTransferDialogConstant.SHORT_CODE_MIN_FONT,
                                maxFontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            ),
                    )
                }
                Text(
                    text = stringResource(Res.string.transfer_code_validity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            is SendTransferState.Request -> {
                val deviceName = state.deviceName.ifBlank { stringResource(Res.string.transfer_request_unknown_device) }
                Text(
                    text = stringResource(Res.string.transfer_request_message, deviceName),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDecline) { Text(stringResource(Res.string.transfer_request_decline)) }
                    Button(onClick = onApprove) { Text(stringResource(Res.string.transfer_request_send)) }
                }
            }

            SendTransferState.Sending -> TransferProgress(Res.string.transfer_sending)

            is SendTransferState.Sent -> TransferOutcome(stringResource(Res.string.transfer_sent, state.deviceName), onClose = onClose)

            SendTransferState.Declined -> TransferOutcome(stringResource(Res.string.transfer_declined_sender), onClose = onClose)

            SendTransferState.Expired ->
                TransferOutcome(
                    stringResource(Res.string.transfer_expired),
                    onClose,
                    onRetry,
                    Res.string.transfer_try_again,
                )

            SendTransferState.NoNetwork ->
                TransferOutcome(
                    stringResource(Res.string.transfer_no_network),
                    onClose,
                    onRetry,
                    Res.string.transfer_try_again,
                )

            SendTransferState.Failed ->
                TransferOutcome(
                    stringResource(Res.string.transfer_failed),
                    onClose,
                    onRetry,
                    Res.string.transfer_try_again,
                )
        }
    }
}

/** The receiving side's screens. [scanner] is a slot so tests can run without a camera. */
@Composable
internal fun ReceiveTransferContent(
    state: ReceiveTransferState,
    scanner: @Composable () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    // Only phones scan; desktop (and any device without a camera) types the short code.
    showScanner: Boolean = isMobilePlatform(),
    onCodeEntered: (String) -> Unit = {},
) {
    TransferScaffold(title = stringResource(Res.string.transfer_receive_action), onClose = onClose) {
        when (state) {
            ReceiveTransferState.Scanning -> {
                if (showScanner) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(DeviceTransferDialogConstant.SCANNER_CORNER)),
                    ) { scanner() }
                    TransferMessage(Res.string.transfer_scan_instructions)
                } else {
                    TransferMessage(Res.string.transfer_type_instructions)
                }
                ManualCodeEntry(onCodeEntered)
            }

            // Received: the restore confirmation takes over as soon as the dialog hands the JSON on.
            ReceiveTransferState.Waiting, is ReceiveTransferState.Received -> TransferProgress(Res.string.transfer_waiting)

            ReceiveTransferState.InvalidCode ->
                TransferOutcome(
                    stringResource(Res.string.transfer_invalid_code),
                    onClose,
                    onRetry,
                    Res.string.transfer_scan_again,
                )

            ReceiveTransferState.Expired ->
                TransferOutcome(
                    stringResource(Res.string.transfer_expired),
                    onClose,
                    onRetry,
                    Res.string.transfer_scan_again,
                )

            ReceiveTransferState.Unreachable ->
                TransferOutcome(
                    stringResource(Res.string.transfer_unreachable),
                    onClose,
                    onRetry,
                    Res.string.transfer_scan_again,
                )

            ReceiveTransferState.Declined -> TransferOutcome(stringResource(Res.string.transfer_declined_receiver), onClose = onClose)

            ReceiveTransferState.Failed ->
                TransferOutcome(
                    stringResource(Res.string.transfer_failed),
                    onClose,
                    onRetry,
                    Res.string.transfer_scan_again,
                )
        }
    }
}

@Composable
private fun ManualCodeEntry(onCodeEntered: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    OutlinedTextField(
        value = code,
        onValueChange = { code = it },
        label = { Text(stringResource(Res.string.transfer_code_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = { onCodeEntered(code) }, enabled = code.isNotBlank()) {
        Text(stringResource(Res.string.transfer_connect))
    }
}

@Composable
private fun TransferScaffold(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_close))
                }
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            ) {
                Column(
                    modifier = Modifier.widthIn(max = DeviceTransferDialogConstant.CONTENT_MAX_WIDTH),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) { content() }
            }
        }
    }
}

@Composable
private fun TransferMessage(text: StringResource) {
    Text(text = stringResource(text), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
}

@Composable
private fun TransferProgress(text: StringResource) {
    CircularProgressIndicator(modifier = Modifier.size(DeviceTransferDialogConstant.PROGRESS_SIZE))
    TransferMessage(text)
}

@Composable
private fun TransferOutcome(
    message: String,
    onClose: () -> Unit,
    onRetry: (() -> Unit)? = null,
    retryLabel: StringResource? = null,
) {
    Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onClose) { Text(stringResource(Res.string.action_close)) }
        if (onRetry != null && retryLabel != null) {
            Button(onClick = onRetry) { Text(stringResource(retryLabel)) }
        }
    }
}
