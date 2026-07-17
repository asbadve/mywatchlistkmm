package com.ajinkyabadve.kmmmywatchlist.features.person.screen

import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonPageResult
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepository
import io.ktor.utils.io.errors.IOException

class FakePersonRepository : PersonRepository {
    var getPopularPeopleResult: Result<PersonPageResult>? = null

    val getPopularPeopleCalls = mutableListOf<Int>()

    override suspend fun getPopularPeople(pageNo: Int): PersonPageResult {
        getPopularPeopleCalls.add(pageNo)

        getPopularPeopleResult?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

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
