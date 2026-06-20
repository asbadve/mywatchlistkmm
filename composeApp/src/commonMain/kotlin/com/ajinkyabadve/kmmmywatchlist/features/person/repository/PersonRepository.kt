package com.ajinkyabadve.kmmmywatchlist.features.person.repository

import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonPageResult

interface PersonRepository {
    suspend fun getPopularPeople(pageNo: Int): PersonPageResult
}
