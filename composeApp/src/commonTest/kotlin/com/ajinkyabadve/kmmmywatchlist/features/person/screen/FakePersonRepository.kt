package com.ajinkyabadve.kmmmywatchlist.features.person.screen

import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonDetail
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonPageResult
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepository
import io.ktor.utils.io.errors.IOException

class FakePersonRepository : PersonRepository {
    var getPopularPeopleResult: Result<PersonPageResult>? = null
    var getPersonDetailsResult: Result<PersonDetail>? = null

    val getPopularPeopleCalls = mutableListOf<Int>()
    val getPersonDetailsCalls = mutableListOf<Long>()

    override suspend fun getPersonDetails(personId: Long): PersonDetail {
        getPersonDetailsCalls.add(personId)

        getPersonDetailsResult?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        return PersonDetail(id = personId, name = "Person A")
    }

    override suspend fun getPopularPeople(pageNo: Int): PersonPageResult {
        getPopularPeopleCalls.add(pageNo)

        getPopularPeopleResult?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        val people =
            listOf(
                Person(id = 301, name = "Person A"),
            )
        return PersonPageResult(
            page = 1,
            list = people,
            totalResults = people.size,
            totalPages = 1,
        )
    }
}
