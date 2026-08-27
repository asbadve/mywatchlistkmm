package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonDetail
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
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
    private val favoritePersonRepository: FavoritePersonRepository = FavoritePersonRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<PersonDetailState>(PersonDetailState.Loading)
    val uiState: StateFlow<PersonDetailState> = _uiState.asStateFlow()

    /** Local-only "favorite person" state - see [FavoritePersonRepository]'s kdoc for why this has
     *  no `account_states`-style pre-check, unlike movie/TV favorites. `FollowPersonButton` (via
     *  [PersonHeroSection]) collects this directly rather than the composable ever touching
     *  [favoritePersonRepository] itself, per code-conventions §6/§8. */
    val isFollowingPerson: Flow<Boolean> = favoritePersonRepository.observeIsFavorite(personId)

    init {
        loadPersonDetails()
    }

    /** [toggleFollowPerson] returns whether this call just turned following ON (`true`) - the
     *  signal `PersonDetailScreen` uses to decide whether to offer the notification opt-in prompt
     *  right there, at the moment of the click (see `NotificationOptInDialog`'s kdoc). Deliberately
     *  not a ViewModel-owned `StateFlow` like `MediaActionsState.shouldPromptForEpisodeAlerts` -
     *  `viewModel(key = "PersonDetailScreenModel:$personId")` can outlive a single screen visit
     *  (this app's `NavDisplay` has no per-entry `ViewModelStore` scoping), so a flag living here
     *  risks surfacing on a later, click-free visit instead of only right after the tap that set
     *  it. Returning the answer directly from the call the click already makes keeps it correct
     *  regardless of how long this ViewModel instance sticks around. */
    fun toggleFollowPerson(
        person: PersonDetail,
        currentlyFollowing: Boolean,
    ): Boolean {
        val newValue = !currentlyFollowing
        viewModelScope.launch {
            favoritePersonRepository.setFavorite(personId, person.name, person.profilePath, newValue)
        }
        return newValue
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
