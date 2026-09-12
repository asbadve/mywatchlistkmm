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
 * A dev-loop shortcut, not this app's real migration mechanism - see [LocalSchemaVersion]'s kdoc
 * for that (per-platform `createSqlDriver` applies `MyDatabase.Schema.migrate()` against real
 * numbered `.sqm` files). This exists for a schema change still in flux, where writing a migration
 * for a shape that's about to change again isn't worth it yet: debug builds only, when the stored
 * version doesn't match [LocalSchemaVersion.CURRENT], wipe the on-disk database and start fresh
 * instead of relying on (or requiring) a migration. Release builds never do this - silently
 * discarding a real user's local data on a version bump is not an acceptable trade.
 */
private fun resetDatabaseIfSchemaChangedForDebug() {
    if (!isDebugBuild()) return
    val settings = createSettings()
    val storedVersion = settings.getInt(KEY_LOCAL_SCHEMA_VERSION, -1)
    if (storedVersion == LocalSchemaVersion.CURRENT) return
    deleteLocalDatabaseFile()
    settings.putInt(KEY_LOCAL_SCHEMA_VERSION, LocalSchemaVersion.CURRENT)
}
