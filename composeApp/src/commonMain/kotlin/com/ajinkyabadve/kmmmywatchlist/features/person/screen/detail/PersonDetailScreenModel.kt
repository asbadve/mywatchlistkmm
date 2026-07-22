package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonDetail
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_person

sealed interface PersonDetailState {
    data object Loading : PersonDetailState

    data class Success(
        val person: PersonDetail,
    ) : PersonDetailState

    data class Error(
        val message: UiText,
    ) : PersonDetailState
}

class PersonDetailScreenModel(
    private val personId: Long,
    private val personRepository: PersonRepository = PersonRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<PersonDetailState>(PersonDetailState.Loading)
    val uiState: StateFlow<PersonDetailState> = _uiState.asStateFlow()

    init {
        loadPersonDetails()
    }

    fun loadPersonDetails() {
        _uiState.value = PersonDetailState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val person = personRepository.getPersonDetails(personId)
                _uiState.value = PersonDetailState.Success(person)
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = TAG, throwable = httpExceptions) {
                    "HTTP Error fetching person details for personId: $personId"
                }
                _uiState.value = PersonDetailState.Error(UiText.Plain(httpExceptions.message))
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) {
                    "IO/Network Error fetching person details for personId: $personId"
                }
                _uiState.value = PersonDetailState.Error(UiText.Resource(Res.string.error_network))
            } catch (e: ContentConvertException) {
                logMalformedResponse(e)
                _uiState.value = PersonDetailState.Error(UiText.Resource(Res.string.error_unexpected_person))
            } catch (e: SerializationException) {
                logMalformedResponse(e)
                _uiState.value = PersonDetailState.Error(UiText.Resource(Res.string.error_unexpected_person))
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private fun logMalformedResponse(throwable: Throwable) {
        Napier.e(tag = TAG, throwable = throwable) {
            "Malformed response while loading person details"
        }
    }

    private companion object {
        const val TAG = "PersonDetailScreenModel"
    }
}
