package com.ajinkyabadve.kmmmywatchlist.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver

private const val DATABASE_FILE_NAME = "mywatchlist.db"

// NativeSqliteDriver over the system libsqlite3 is synchronous - same .synchronous() adapter as
// the Android actual, for the same reason (this database's schema is async to support the JS
// WebWorkerDriver). Auto-creates the schema on construction.
internal actual suspend fun createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver =
    NativeSqliteDriver(schema.synchronous(), DATABASE_FILE_NAME)
