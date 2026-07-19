package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonDetail
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PersonDetailState {
    data object Loading : PersonDetailState
    data class Success(val person: PersonDetail) : PersonDetailState
    data class Error(val message: String) : PersonDetailState
}

class PersonDetailScreenModel(
    private val personId: Long,
    private val personRepository: PersonRepository = PersonRepositoryImpl()
) : ViewModel() {

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<PersonDetailState>(PersonDetailState.Loading)
    val uiState: StateFlow<PersonDetailState> = _uiState.asStateFlow()

    init {
        loadPersonDetails()
    }

    @Suppress("detekt:TooGenericExceptionCaught")
    fun loadPersonDetails() {
        _uiState.value = PersonDetailState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val person = personRepository.getPersonDetails(personId)
                _uiState.value = PersonDetailState.Success(person)
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = "PersonDetailScreenModel", throwable = httpExceptions) {
                    "HTTP Error fetching person details for personId: $personId"
                }
                _uiState.value = PersonDetailState.Error(httpExceptions.message)
            } catch (e: IOException) {
                Napier.e(tag = "PersonDetailScreenModel", throwable = e) {
                    "IO/Network Error fetching person details for personId: $personId"
                }
                _uiState.value = PersonDetailState.Error("Network Connection Error. Please check your internet connectivity.")
            } catch (e: Exception) {
                Napier.e(tag = "PersonDetailScreenModel", throwable = e) {
                    "Unexpected Error fetching person details for personId: $personId"
                }
                _uiState.value = PersonDetailState.Error("An unexpected error occurred while loading the person. Please try again.")
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }
}
