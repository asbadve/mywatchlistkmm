@file:JvmName("DatabaseDriverFactoryDesktop")

package com.ajinkyabadve.kmmmywatchlist.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import java.nio.file.Path

private const val APP_DATA_DIR_NAME = ".mywatchlist"
private const val DATABASE_FILE_NAME = "mywatchlist.db"
private const val PRAGMA_USER_VERSION_READ = "PRAGMA user_version"

// JdbcSqliteDriver's own create/query calls are synchronous under the hood regardless of the
// async schema type, so - unlike Android/Native - this platform calls the async schema.create()/
// migrate() directly (matching SQLDelight's own JVM multiplatform example) rather than adapting
// them first.
//
// Unlike AndroidSqliteDriver/NativeSqliteDriver (which wrap a versioned schema and run
// create()/migrate() automatically via the platform's own onCreate/onUpgrade-style callback),
// JdbcSqliteDriver does nothing on its own - this actual owns that bookkeeping by hand, using
// SQLite's built-in `PRAGMA user_version` integer as the on-disk version marker (the same
// mechanism Android's SQLiteOpenHelper uses internally). A schema-touching change to
// `MyDatabase.sq` from here on must ship a matching numbered `.sqm` migration file (see
// `LocalSchemaVersion`'s kdoc) - without one, `schema.migrate()` has nothing to apply and this
// still leaves the on-disk file on its old, now-incompatible shape.
private fun databaseFilePath(): Path = Path.of(System.getProperty("user.home"), APP_DATA_DIR_NAME).resolve(DATABASE_FILE_NAME)

internal actual suspend fun createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
    val databasePath = databaseFilePath()
    Files.createDirectories(databasePath.parent)
    val isNewDatabase = !Files.exists(databasePath)
    val driver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")
    if (isNewDatabase) {
        schema.create(driver).await()
        driver.setUserVersion(schema.version)
    } else {
        val onDiskVersion = driver.readUserVersion()
        if (onDiskVersion < schema.version) {
            schema.migrate(driver, onDiskVersion, schema.version).await()
            driver.setUserVersion(schema.version)
        }
    }
    return driver
}

private suspend fun SqlDriver.readUserVersion(): Long =
    executeQuery(
        null,
        PRAGMA_USER_VERSION_READ,
        { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        0,
        null,
    ).await()

private suspend fun SqlDriver.setUserVersion(version: Long) {
    execute(null, "PRAGMA user_version = $version", 0).await()
}

internal actual fun deleteLocalDatabaseFile() {
    Files.deleteIfExists(databaseFilePath())
}
