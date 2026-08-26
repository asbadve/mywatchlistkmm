package com.ajinkyabadve.kmmmywatchlist.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.ajinkyabadve.kmmmywatchlist.createSettings
import com.ajinkyabadve.kmmmywatchlist.isDebugBuild

private const val KEY_LOCAL_SCHEMA_VERSION = "local_schema_version"

/**
 * Constructs (and, on first run, creates) this platform's [SqlDriver] for [MyDatabase]. Mirrors
 * the `createSettings()`/`WebAuthLauncher` `expect`/`actual` pattern already used elsewhere in
 * this codebase for other per-platform primitives - no DI framework involved.
 *
 * The database is async (`generateAsync = true` in the Gradle config, required by the JS
 * `WebWorkerDriver`), so every platform receives the same [SqlSchema.Companion]-shaped async
 * schema and adapts it as its own driver needs - see each `actual` for how.
 */
internal expect suspend fun createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver

/** Deletes this platform's on-disk database file, if any - see [resetDatabaseIfSchemaChangedForDebug].
 *  A no-op where there's nothing durable to delete (JS's `WebWorkerDriver` is in-memory per page
 *  load already) or where the on-disk location isn't reliably known yet (iOS - see its `actual`). */
internal expect fun deleteLocalDatabaseFile()

/** The single entry point repositories use to obtain a ready-to-query [MyDatabase] instance. */
internal suspend fun createDatabase(): MyDatabase {
    resetDatabaseIfSchemaChangedForDebug()
    return MyDatabase(createSqlDriver(MyDatabase.Schema))
}

/**
 * This project has no real SQLDelight migration path (see [LocalSchemaVersion]'s kdoc) - without
 * this, every schema-touching change to `MyDatabase.sq` crashed with `SQLiteException: no such
 * table/column` the first time a query touched it, requiring a manual `pm clear`/reinstall.
 * Debug builds only: when the stored version doesn't match [LocalSchemaVersion.CURRENT], wipe the
 * on-disk database and start fresh instead. Release builds never do this - silently discarding a
 * real user's local data on a version bump is not an acceptable trade even for the same crash.
 */
private fun resetDatabaseIfSchemaChangedForDebug() {
    if (!isDebugBuild()) return
    val settings = createSettings()
    val storedVersion = settings.getInt(KEY_LOCAL_SCHEMA_VERSION, -1)
    if (storedVersion == LocalSchemaVersion.CURRENT) return
    deleteLocalDatabaseFile()
    settings.putInt(KEY_LOCAL_SCHEMA_VERSION, LocalSchemaVersion.CURRENT)
}
