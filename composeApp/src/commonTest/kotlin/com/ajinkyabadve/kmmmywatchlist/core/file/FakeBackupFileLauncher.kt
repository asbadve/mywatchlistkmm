package com.ajinkyabadve.kmmmywatchlist.core.file

/** In-memory stand-in for the platform file pickers: [openResult] is what "picking a file"
 *  returns (null = cancelled), and every save is recorded instead of written. */
class FakeBackupFileLauncher(
    var openResult: String? = null,
    var saveSucceeds: Boolean = true,
) : BackupFileLauncher {
    val saved = mutableListOf<Pair<String, String>>()

    override suspend fun save(
        suggestedFileName: String,
        content: String,
    ): Boolean {
        saved.add(suggestedFileName to content)
        return saveSucceeds
    }

    override suspend fun open(): String? = openResult
}
