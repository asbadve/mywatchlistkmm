package com.ajinkyabadve.kmmmywatchlist.features.person.screen

import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonPageResult
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepository

class FakePersonRepository : PersonRepository {
    override suspend fun getPopularPeople(pageNo: Int): PersonPageResult {
        val people = listOf(
            Person(id = 301, name = "Person A")
        )
        return PersonPageResult(
            page = 1,
            list = people,
            totalResults = people.size,
            totalPages = 1
        )
    }
}
