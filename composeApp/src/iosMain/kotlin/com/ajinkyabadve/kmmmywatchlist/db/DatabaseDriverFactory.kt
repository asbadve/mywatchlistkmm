package com.ajinkyabadve.kmmmywatchlist.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

private const val DATABASE_FILE_NAME = "mywatchlist.db"

// NativeSqliteDriver over the system libsqlite3 is synchronous - same .synchronous() adapter as
// the Android actual, for the same reason (this database's schema is async to support the JS
// WebWorkerDriver). Auto-creates the schema on construction.
internal actual suspend fun createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver =
    NativeSqliteDriver(schema.synchronous(), DATABASE_FILE_NAME)

// NativeSqliteDriver's default DatabaseManager (SQLDelight's own, not something this app
// configures) resolves relative filenames under `<Application Support>/databases/<name>` -
// confirmed 2026-08-26 by inspecting a real simulator container
// (`xcrun simctl get_app_container ... data`), not just inferred from docs. Also removes the
// `-wal`/`-shm` WAL-mode companion files SQLite creates alongside the main file - leaving those
// behind would let stale journal data resurrect part of the schema this delete is meant to erase.
@OptIn(ExperimentalForeignApi::class)
internal actual fun deleteLocalDatabaseFile() {
    val appSupportDir =
        NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String
            ?: return
    val basePath = "$appSupportDir/databases/$DATABASE_FILE_NAME"
    val fileManager = NSFileManager.defaultManager
    listOf(basePath, "$basePath-wal", "$basePath-shm").forEach { path ->
        fileManager.removeItemAtPath(path, null)
    }
}
