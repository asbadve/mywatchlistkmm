@file:JvmName("DatabaseDriverFactoryAndroid")

package com.ajinkyabadve.kmmmywatchlist.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.ajinkyabadve.kmmmywatchlist.AndroidApp

private const val DATABASE_FILE_NAME = "mywatchlist.db"

// AndroidSqliteDriver wraps the platform's own (synchronous) SQLite - .synchronous() adapts the
// async schema this database is generated with (required for the JS WebWorkerDriver) back to the
// blocking form this driver expects, and the driver auto-creates the schema on construction.
internal actual suspend fun createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver =
    AndroidSqliteDriver(schema.synchronous(), AndroidApp.instance, DATABASE_FILE_NAME)
