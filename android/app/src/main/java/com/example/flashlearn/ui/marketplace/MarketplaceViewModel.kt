package com.example.flashlearn.ui.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.data.remote.dto.MarketplaceDeckDto
import com.example.flashlearn.data.repository.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Slug kategorii + czytelna etykieta dla chipsa filtrowania. */
data class CategoryFilter(val slug: String?, val labelRes: Int)

data class MarketplaceUiState(
    val isLoading: Boolean = false,
    val decks: List<MarketplaceDeckDto> = emptyList(),
    val selectedCategory: String? = null,   // null = „Wszystkie"
    val cloningId: Long? = null,            // id talii w trakcie klonowania
    val cloneSuccessId: Long? = null,       // id właśnie sklonowanej talii (snackbar)
    val error: String? = null
)

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val repository: MarketplaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    init {
        loadDecks()
    }

    fun selectCategory(slug: String?) {
        if (_uiState.value.selectedCategory == slug) return
        _uiState.update { it.copy(selectedCategory = slug) }
        loadDecks()
    }

    fun refresh() = loadDecks()

    fun cloneDeck(deckId: Long) {
        if (_uiState.value.cloningId != null) return // blokada równoczesnych klonowań
        viewModelScope.launch {
            _uiState.update { it.copy(cloningId = deckId) }
            runCatching { repository.cloneDeck(deckId) }
                .onSuccess {
                    _uiState.update { it.copy(cloningId = null, cloneSuccessId = deckId) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(cloningId = null, error = e.message) }
                }
        }
    }

    fun clearCloneSuccess() = _uiState.update { it.copy(cloneSuccessId = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun loadDecks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getPublicDecks(_uiState.value.selectedCategory) }
                .onSuccess { decks ->
                    _uiState.update { it.copy(isLoading = false, decks = decks) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
