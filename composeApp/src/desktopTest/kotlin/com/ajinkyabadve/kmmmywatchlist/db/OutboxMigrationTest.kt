package com.ajinkyabadve.kmmmywatchlist.db

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `2.sqm` on a real version-2 database (the `2.db` schema snapshot): an offline removal stuck from
 * before the outbox (`pendingSync = 1`, never sent) becomes a queued, unclaimed removal, and a
 * normal row queues nothing. `verifyCommonMainMyDatabaseMigration` already checks the resulting
 * schema; this checks the data step.
 */
class OutboxMigrationTest {
    @Test
    fun testMigrationQueuesStuckRemovalsAsUnclaimedRemovals() =
        runTest {
            val file = Files.createTempFile(TEMP_PREFIX, TEMP_SUFFIX)
            try {
                Files.copy(Path.of(SNAPSHOT_V2), file, StandardCopyOption.REPLACE_EXISTING)
                val driver = JdbcSqliteDriver(JDBC_PREFIX + file)
                driver.execute(null, insertRow(STUCK_ID, pendingSync = 1), 0).await()
                driver.execute(null, insertRow(SYNCED_ID, pendingSync = 0), 0).await()

                MyDatabase.Schema.migrate(driver, VERSION_BEFORE, MyDatabase.Schema.version).await()

                val queued = MyDatabase(driver).myDatabaseQueries.selectOutboxEntriesForAccount(ANY_ACCOUNT_ID).awaitAsList()
                assertEquals(listOf(STUCK_ID), queued.map { it.id })
                assertEquals(UNCLAIMED_ACCOUNT_ID, queued.single().accountId)
                assertEquals(DESIRED_REMOVE, queued.single().desiredState)
                driver.close()
            } finally {
                Files.deleteIfExists(file)
            }
        }

    private fun insertRow(
        id: Long,
        pendingSync: Int,
    ) = "INSERT INTO trackedMedia(id, mediaType, category, title, addedAt, lastSyncedAt, isDeleted, pendingSync) " +
        "VALUES ($id, 'movie', 'favorite', 'Title $id', 0, 0, $pendingSync, $pendingSync)"

    private companion object {
        // Gradle runs desktopTest from the composeApp module directory.
        const val SNAPSHOT_V2 = "src/commonMain/sqldelight/databases/2.db"
        const val TEMP_PREFIX = "outbox-migration"
        const val TEMP_SUFFIX = ".db"
        const val JDBC_PREFIX = "jdbc:sqlite:"
        const val VERSION_BEFORE = 2L
        const val STUCK_ID = 5L
        const val SYNCED_ID = 6L
        const val ANY_ACCOUNT_ID = 1L
        const val UNCLAIMED_ACCOUNT_ID = 0L
        const val DESIRED_REMOVE = 0L
    }
}
