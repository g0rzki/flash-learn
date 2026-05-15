package com.example.flashlearn.ui.publish

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.data.remote.dto.CategoryDto
import com.example.flashlearn.data.repository.CategoryRepository
import com.example.flashlearn.data.repository.MarketplaceRepository
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.DeckWithCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PublishUiState {
    /** Trwa ładowanie kategorii / talii */
    object Loading : PublishUiState

    /** Dane załadowane — formularz gotowy */
    data class Ready(
        val deckTitle: String,
        val deckServerId: Long?,          // null gdy talia nie jest zsynchronizowana
        val categories: List<CategoryDto>,
        val selectedCategoryId: Long?,
        val description: String,
        val isSubmitting: Boolean = false
    ) : PublishUiState

    /** Publikacja zakończyła się sukcesem */
    object Success : PublishUiState

    /** Błąd ładowania */
    data class Error(val message: String) : PublishUiState
}

@HiltViewModel
class PublishDeckViewModel @Inject constructor(
    private val deckDao: DeckDao,
    private val categoryRepository: CategoryRepository,
    private val marketplaceRepository: MarketplaceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    private val _uiState = MutableStateFlow<PublishUiState>(PublishUiState.Loading)
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    /** Jednorazowe zdarzenie błędu submitu (do Snackbar) */
    private val _submitError = MutableStateFlow<String?>(null)
    val submitError: StateFlow<String?> = _submitError.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching {
                // Pełna encja Deck (zawiera serverId)
                val deck = deckDao.getById(deckId)
                    ?: error("Talia o id=$deckId nie istnieje")
                val categories = categoryRepository.getCategories()
                Pair(deck, categories)
            }.onSuccess { (deck, categories) ->
                _uiState.value = PublishUiState.Ready(
                    deckTitle = deck.title,
                    deckServerId = deck.serverId,
                    categories = categories,
                    selectedCategoryId = categories.firstOrNull()?.id,
                    description = deck.description.orEmpty()
                )
            }.onFailure { e ->
                _uiState.value = PublishUiState.Error(
                    e.message ?: "Błąd ładowania danych"
                )
            }
        }
    }

    fun selectCategory(categoryId: Long) {
        val s = _uiState.value as? PublishUiState.Ready ?: return
        _uiState.value = s.copy(selectedCategoryId = categoryId)
    }

    fun setDescription(text: String) {
        val s = _uiState.value as? PublishUiState.Ready ?: return
        _uiState.value = s.copy(description = text)
    }

    fun submit() {
        val s = _uiState.value as? PublishUiState.Ready ?: return
        val categoryId = s.selectedCategoryId ?: run {
            _submitError.value = "Wybierz kategorię"
            return
        }
        val serverId = s.deckServerId ?: run {
            _submitError.value = "Talia nie jest jeszcze zsynchronizowana z serwerem. Spróbuj za chwilę."
            return
        }

        _uiState.value = s.copy(isSubmitting = true)

        viewModelScope.launch {
            runCatching {
                marketplaceRepository.submitDeck(
                    deckServerId = serverId,
                    categoryId = categoryId,
                    description = s.description
                )
            }.onSuccess {
                _uiState.value = PublishUiState.Success
            }.onFailure { e ->
                // Przywróć stan Ready, przekaż błąd przez Snackbar
                _uiState.value = s.copy(isSubmitting = false)
                _submitError.value = e.message ?: "Publikacja nie powiodła się"
            }
        }
    }

    fun clearSubmitError() {
        _submitError.value = null
    }
}
