package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase

internal actual fun createTrackedMediaRepository(
    accountMediaRepository: AccountMediaRepository,
    databaseProvider: suspend () -> MyDatabase,
): TrackedMediaRepository = SqliteTrackedMediaRepositoryImpl(accountMediaRepository, databaseProvider)

internal actual fun createCustomListRepository(
    listsRepository: ListsRepository,
    databaseProvider: suspend () -> MyDatabase,
): CustomListRepository = SqliteCustomListRepositoryImpl(listsRepository, databaseProvider)
