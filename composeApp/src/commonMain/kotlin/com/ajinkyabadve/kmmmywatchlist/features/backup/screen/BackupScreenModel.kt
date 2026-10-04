package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.backup.model.BackupEnvelope
import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.BackupConstant
import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.BackupDecodeResult
import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.BackupRepository
import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.BackupRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.BackupSummary
import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.RestoreOutcome
import com.ajinkyabadve.kmmmywatchlist.features.notifications.NotificationJobSync
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderCoordinator
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** The backup flow's steps. Results are dialogs, not snackbars - this app has no snackbar host. */
sealed interface BackupUiState {
    data object Idle : BackupUiState

    /** The Export / Restore choice dialog. */
    data object Choosing : BackupUiState

    data object Working : BackupUiState

    data class ConfirmRestore(
        val envelope: BackupEnvelope,
        val summary: BackupSummary,
    ) : BackupUiState

    data object Exported : BackupUiState

    data object ExportFailed : BackupUiState

    data class Restored(
        val outcome: RestoreOutcome,
    ) : BackupUiState

    data class UnsupportedFormat(
        val found: Int,
        val supported: Int,
    ) : BackupUiState

    data object Malformed : BackupUiState
}

/**
 * Drives item 15's export/restore. The file picking itself happens in the composable (it needs a
 * composition-registered launcher), which hands the results here.
 *
 * A restore always goes through [BackupUiState.ConfirmRestore] first. Afterwards it reschedules
 * the OS reminder notifications and the shared background job, since restored reminders and a
 * restored episode-alerts setting both change what should be scheduled.
 */
class BackupScreenModel(
    private val backupRepository: BackupRepository = BackupRepositoryImpl(),
    releaseReminderRepository: ReleaseReminderRepository = ReleaseReminderRepositoryImpl(),
    private val releaseReminderCoordinator: ReleaseReminderCoordinator = ReleaseReminderCoordinator(releaseReminderRepository),
    private val notificationJobSync: NotificationJobSync = NotificationJobSync(releaseReminderRepository = releaseReminderRepository),
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun open() {
        _uiState.value = BackupUiState.Choosing
    }

    fun dismiss() {
        _uiState.value = BackupUiState.Idle
    }

    /** `mywatchlist-backup-2026-10-04.json`. */
    fun suggestedFileName(): String = BackupConstant.FILE_NAME_PREFIX + today() + BackupConstant.FILE_EXTENSION

    suspend fun buildExport(): String {
        _uiState.value = BackupUiState.Working
        return backupRepository.exportToJson()
    }

    fun onExportFinished(saved: Boolean) {
        _uiState.value = if (saved) BackupUiState.Exported else BackupUiState.ExportFailed
    }

    /** [content] is null when the user cancelled the picker - back to the choice, no error. */
    fun onFilePicked(content: String?) {
        if (content == null) {
            _uiState.value = BackupUiState.Choosing
            return
        }
        _uiState.value =
            when (val result = backupRepository.decode(content)) {
                is BackupDecodeResult.Valid -> BackupUiState.ConfirmRestore(result.envelope, result.summary)
                is BackupDecodeResult.UnsupportedFormat -> BackupUiState.UnsupportedFormat(result.found, result.supported)
                BackupDecodeResult.Malformed -> BackupUiState.Malformed
            }
    }

    fun confirmRestore() {
        val state = _uiState.value as? BackupUiState.ConfirmRestore ?: return
        _uiState.value = BackupUiState.Working
        viewModelScope.launch {
            val outcome = backupRepository.restore(state.envelope)
            releaseReminderCoordinator.rescheduleAll()
            notificationJobSync.refresh()
            _uiState.value = BackupUiState.Restored(outcome)
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}
