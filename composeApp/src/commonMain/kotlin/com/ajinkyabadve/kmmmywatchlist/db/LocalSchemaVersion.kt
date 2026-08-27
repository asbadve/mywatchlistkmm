package com.ajinkyabadve.kmmmywatchlist.db

/**
 * Manually bumped every time `MyDatabase.sq` changes in a way an existing on-disk database can't
 * just tolerate (a new table, a new column a query now selects, a changed query shape) - this
 * project has no real SQLDelight migration path yet (see `future_features_checklist.md` item 2's
 * known limitation), so past schema changes crashed with `SQLiteException: no such table/column`
 * the first time a query touched what changed.
 *
 * [AppDatabaseProvider] compares this against a stored value (`multiplatform-settings`, same store
 * everything else in this app uses) on startup and, **debug builds only**, wipes the on-disk
 * database and starts fresh instead of crashing when they don't match - see its kdoc. Release
 * builds never do this; shipping a real migration is still a prerequisite for a release with local
 * storage, this only removes the need to manually `pm clear`/delete-and-reinstall after every
 * schema-touching change during development.
 */
internal object LocalSchemaVersion {
    const val CURRENT = 2
}
