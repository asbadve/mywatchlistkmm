package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase

// The web target never touches [databaseProvider] - see NetworkOnlyTrackedMediaRepositoryImpl's
// kdoc for why it skips SQLite entirely rather than fighting sql.js/WebWorkerDriver's build cost.
internal actual fun createTrackedMediaRepository(
    accountMediaRepository: AccountMediaRepository,
    databaseProvider: suspend () -> MyDatabase,
): TrackedMediaRepository = NetworkOnlyTrackedMediaRepositoryImpl(accountMediaRepository)

internal actual fun createCustomListRepository(
    listsRepository: ListsRepository,
    databaseProvider: suspend () -> MyDatabase,
): CustomListRepository = NetworkOnlyCustomListRepositoryImpl(listsRepository)
