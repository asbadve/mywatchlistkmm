package com.ajinkyabadve.kmmmywatchlist.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

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

/** The single entry point repositories use to obtain a ready-to-query [MyDatabase] instance. */
internal suspend fun createDatabase(): MyDatabase = MyDatabase(createSqlDriver(MyDatabase.Schema))
