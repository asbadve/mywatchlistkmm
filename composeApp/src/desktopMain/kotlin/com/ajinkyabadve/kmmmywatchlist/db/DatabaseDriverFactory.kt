@file:JvmName("DatabaseDriverFactoryDesktop")

package com.ajinkyabadve.kmmmywatchlist.db

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import java.nio.file.Path

private const val APP_DATA_DIR_NAME = ".mywatchlist"
private const val DATABASE_FILE_NAME = "mywatchlist.db"

// JdbcSqliteDriver's own create/query calls are synchronous under the hood regardless of the
// async schema type, so - unlike Android/Native - this platform calls the async schema.create()
// directly (matching SQLDelight's own JVM multiplatform example) rather than adapting it first.
// Unlike AndroidSqliteDriver/NativeSqliteDriver (which auto-create the schema only when it's
// actually missing), JdbcSqliteDriver does nothing on its own - schema.create() must be called
// explicitly, but only for a genuinely new file. Calling it against an existing database (every
// relaunch after the first) throws "table ... already exists"; there's no schema-version-based
// migration here yet (see docs/local-storage-plan.html), so a file-existence check is the
// simplest correct guard until one exists.
private fun databaseFilePath(): Path = Path.of(System.getProperty("user.home"), APP_DATA_DIR_NAME).resolve(DATABASE_FILE_NAME)

internal actual suspend fun createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
    val databasePath = databaseFilePath()
    Files.createDirectories(databasePath.parent)
    val isNewDatabase = !Files.exists(databasePath)
    val driver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")
    if (isNewDatabase) {
        schema.create(driver).await()
    }
    return driver
}

internal actual fun deleteLocalDatabaseFile() {
    Files.deleteIfExists(databaseFilePath())
}
