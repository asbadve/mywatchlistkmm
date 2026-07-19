package com.ajinkyabadve.kmmmywatchlist.features.person.repository

import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonDetail
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonPageResult
import com.ajinkyabadve.kmmmywatchlist.network.builder.mediaHttpBuilder
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import kotlinproject.composeapp.BuildConfig

class PersonRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : PersonRepository {
    override suspend fun getPopularPeople(pageNo: Int): PersonPageResult {
        val response: HttpResponse = tmdbClient.client.get {
            mediaHttpBuilder(POPULAR, pageNo.toString(), PERSON)
        }
        return response.body()
    }

    override suspend fun getPersonDetails(personId: Long): PersonDetail {
        val response: HttpResponse = tmdbClient.client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = NetworkConstant.HOST
                trailingQuery = true
                encodedPath = "$PERSON$personId"
                parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                parameters.append("append_to_response", APPEND_TO_RESPONSE)
            }
        }
        return response.body()
    }

    private companion object {
        const val PERSON = "/3/person/"
        const val POPULAR = "popular"
        const val APPEND_TO_RESPONSE = "combined_credits,external_ids,images,translations"
    }
}
