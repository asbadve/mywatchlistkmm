package com.ajinkyabadve.kmmmywatchlist.db

/**
 * Kept equal to `MyDatabase.Schema.version` (SQLDelight's own on-disk schema version, driven by
 * the count of numbered `.sqm` migration files next to `MyDatabase.sq` - see
 * `docs/ci-yml-explained.md`/this project's release process for the full migration workflow).
 * Reset to `1` for the v1.0.0 release: every prior bump (1-3) predated any real migration path and
 * only ever tracked debug-build wipes, not a real shipped schema - no release before this one ever
 * existed for a real on-disk database to migrate *from*.
 *
 * From this release on, a schema-touching change to `MyDatabase.sq` (a new table, a new column a
 * query now selects, a changed query shape) must ship **both**:
 * 1. A new `N.sqm` file (SQLDelight migration syntax) that ALTER/CREATEs an existing database on
 *    version `N-1` up to what `MyDatabase.sq` now describes. Turn on `verifyMigrations.set(true)`
 *    (`composeApp/build.gradle.kts`'s `sqldelight` block, currently commented out - see its own
 *    comment for why) alongside adding this first `.sqm` file, so the build actually fails if it
 *    doesn't produce exactly that.
 * 2. This constant, bumped to match. [AppDatabaseProvider]/`DatabaseDriverFactory` per platform
 *    apply the real migration on launch (Android/iOS via `AndroidSqliteDriver`/`NativeSqliteDriver`
 *    internally; Desktop by hand against `PRAGMA user_version` - see its `DatabaseDriverFactory`
 *    kdoc; JS never persists, so it's a no-op there) - a real user's data on-disk is never wiped
 *    for this.
 *
 * The **debug-build-only** convenience wipe this constant used to solely exist for still exists
 * (see [AppDatabaseProvider]'s kdoc) as a dev-loop shortcut for skipping writing a migration file
 * while a schema change is still in flux - it fires only when a debug build's stored value doesn't
 * match `CURRENT`, independent of whether a real `.sqm` migration exists yet.
 */
internal object LocalSchemaVersion {
    const val CURRENT = 1
}
