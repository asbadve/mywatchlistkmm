package com.ajinkyabadve.kmmmywatchlist.features.person.repository

import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonPageResult
import com.ajinkyabadve.kmmmywatchlist.network.builder.mediaHttpBuilder
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

class PersonRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : PersonRepository {
    override suspend fun getPopularPeople(pageNo: Int): PersonPageResult {
        val response: HttpResponse = tmdbClient.client.get {
            mediaHttpBuilder(POPULAR, pageNo.toString(), PERSON)
        }
        return response.body()
    }

    private companion object {
        const val PERSON = "/3/person/"
        const val POPULAR = "popular"
    }
}
