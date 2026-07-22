package com.ajinkyabadve.kmmmywatchlist.features.person.screen.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import com.ajinkyabadve.kmmmywatchlist.network.isServerError
import io.github.aakira.napier.log
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

class PersonListScreenModel(
    private val personRepository: PersonRepository = PersonRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    internal val personList = mutableStateListOf<Person>()

    private var page by mutableStateOf(1)
    private var canPaginate by mutableStateOf(false)
    var listState by mutableStateOf(ListState.IDLE)

    init {
        loadPopularPeople()
    }

    internal fun loadPopularPeople() {
        viewModelScope.launch(Dispatchers.Main) {
            if (isFirstPage() || isNotFirstPageAndCanPaginate() && (isListStateLoadable())) {
                listState = if (isFirstPage()) ListState.LOADING else ListState.PAGINATING

                try {
                    val response = personRepository.getPopularPeople(page)
                    response.list?.let {
                        canPaginate = response.page <= (response.totalPages ?: 0)
                        if (isFirstPage()) {
                            personList.clear()
                            personList.addAll(it)
                        } else {
                            personList.addAll(it)
                        }
                    }
                    listState =
                        if (canPaginate) {
                            page++
                            ListState.IDLE
                        } else {
                            ListState.PAGINATION_EXHAUST
                        }
                } catch (httpExceptions: HttpExceptions) {
                    httpExceptions.printStackTrace()
                    listState =
                        if (httpExceptions.isServerError()) {
                            ListState.NETWORK_ERROR
                        } else {
                            ListState.ERROR
                        }
                } catch (e: IOException) {
                    e.printStackTrace()
                    log { "IOException" }
                    listState = ListState.NETWORK_ERROR
                } catch (e: ContentConvertException) {
                    log { "Malformed response: ${e.message}" }
                    listState = ListState.ERROR
                } catch (e: SerializationException) {
                    log { "Malformed response: ${e.message}" }
                    listState = ListState.ERROR
                }
            }
        }
    }

    private fun isListStateLoadable() = isListStateIdle() || isListStateNetworkError()

    private fun isNotFirstPageAndCanPaginate() = (page != 1 && canPaginate)

    private fun isListStateIdle() = listState == ListState.IDLE

    private fun isListStateNetworkError() = listState == ListState.NETWORK_ERROR

    private fun isFirstPage() = page == 1

    override fun onCleared() {
        page = 1
        listState = ListState.IDLE
        canPaginate = false
        viewModelScope.cancel()
        super.onCleared()
    }
}
