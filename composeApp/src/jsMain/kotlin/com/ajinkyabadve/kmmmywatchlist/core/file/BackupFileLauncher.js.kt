package com.ajinkyabadve.kmmmywatchlist.core.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Never reached: backup is hidden on the web target (item 15 - its database is in-memory per page
 *  load, so there is almost nothing to back up). Web support waits for item 18's backend. */
private class UnsupportedBackupFileLauncher : BackupFileLauncher {
    override suspend fun save(
        suggestedFileName: String,
        content: String,
    ): Boolean = false

    override suspend fun open(): String? = null
}

@Composable
actual fun rememberBackupFileLauncher(): BackupFileLauncher = remember { UnsupportedBackupFileLauncher() }
