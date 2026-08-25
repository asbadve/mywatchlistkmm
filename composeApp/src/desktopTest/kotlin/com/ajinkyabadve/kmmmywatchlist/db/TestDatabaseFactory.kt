package com.ajinkyabadve.kmmmywatchlist.db

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

/** A fresh in-memory [MyDatabase] per call - isolates each test from the shared app singleton. */
internal suspend fun createTestDatabase(): MyDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    MyDatabase.Schema.create(driver).await()
    return MyDatabase(driver)
}
