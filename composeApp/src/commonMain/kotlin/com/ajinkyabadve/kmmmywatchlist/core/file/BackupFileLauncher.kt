package com.ajinkyabadve.kmmmywatchlist.core.file

import androidx.compose.runtime.Composable

/**
 * Saves a backup to, and reads one from, a file the user picks (future_features_checklist.md
 * item 15).
 *
 * Platform equivalent checked first, per the code conventions: Compose Multiplatform 1.11.1 has no
 * common file dialog, and FileKit (the multiplatform wrapper) needs Compose Multiplatform 1.12. So
 * each platform calls its own native picker: Android's Storage Access Framework
 * (`CreateDocument`/`OpenDocument`, no storage permission), iOS's `UIDocumentPickerViewController`,
 * and `java.awt.FileDialog` on desktop. The web target has no backup (the row is hidden there).
 *
 * A `@Composable remember…()` returning an interface, for the same reason as
 * `rememberNotificationPermissionRequester()`: Android's pickers need an Activity result launcher
 * registered from composition.
 */
interface BackupFileLauncher {
    /** Asks where to save [content]; true once written, false if cancelled or the write failed. */
    suspend fun save(
        suggestedFileName: String,
        content: String,
    ): Boolean

    /** Asks for a file and returns its text, or null if cancelled or it couldn't be read. */
    suspend fun open(): String?
}

@Composable
expect fun rememberBackupFileLauncher(): BackupFileLauncher
