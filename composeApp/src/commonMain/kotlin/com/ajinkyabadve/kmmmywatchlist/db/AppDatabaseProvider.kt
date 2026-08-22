package com.ajinkyabadve.kmmmywatchlist.db

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One [MyDatabase]/driver for the whole app, built lazily on first use. Driver construction is
 * `suspend` (the JS `WebWorkerDriver` needs to await schema creation - see
 * `db/DatabaseDriverFactory.kt`'s platform `actual`s), so this can't be a plain `by lazy {}`;
 * the mutex only guards the handful of concurrent-first-call races, not steady-state reads.
 */
internal object AppDatabaseProvider {
    private var database: MyDatabase? = null
    private val mutex = Mutex()

    suspend fun get(): MyDatabase {
        database?.let { return it }
        return mutex.withLock {
            database ?: createDatabase().also { database = it }
        }
    }
}
