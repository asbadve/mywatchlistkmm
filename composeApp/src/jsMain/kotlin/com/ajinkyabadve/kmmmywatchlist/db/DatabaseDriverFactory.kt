package com.ajinkyabadve.kmmmywatchlist.db

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

// The one target this project's own local-storage plan (docs/local-storage-plan.html) flags as
// best-effort rather than production-ready: SQL.js's worker runs the database in memory unless
// it's explicitly told to persist (IndexedDB/OPFS), which this driver does not do on its own.
// Ship it as-is for now - a fresh page load may lose local data on this platform specifically -
// and revisit if/when SQLDelight's own JS persistence story matures. The URL must be built
// entirely inside this js() block (import.meta.url) for Webpack to resolve it while bundling -
// see https://sqldelight.github.io/sqldelight/2.3.2/js_sqlite/.
internal actual suspend fun createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
    val worker = Worker(js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)"""))
    val driver = WebWorkerDriver(worker)
    schema.create(driver).await()
    return driver
}

// No-op: this driver is in-memory only per page load (see the kdoc above) - there's never a stale
// on-disk file to delete on this platform in the first place.
internal actual fun deleteLocalDatabaseFile() {
}
