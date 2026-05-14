package com.example.flashlearn.ui.deckdetail

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashlearn.data.dao.DeckWithCount
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.FlashcardDao
import com.flashlearn.data.dao.FlashcardProgressDao
import com.flashlearn.data.entity.Flashcard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class FlashcardSortOrder {
    DATE_DESC,
    ALPHABETICAL,
    DIFFICULTY_ASC
}

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
    private val flashcardProgressDao: FlashcardProgressDao,
    private val prefs: SharedPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])
    private val SORT_PREFS_KEY = "flashcard_sort_order_deck_$deckId"

    private val _deck = MutableStateFlow<DeckWithCount?>(null)
    val deck: StateFlow<DeckWithCount?> = _deck

    private val _sortOrder = MutableStateFlow(
        FlashcardSortOrder.valueOf(
            prefs.getString(SORT_PREFS_KEY, FlashcardSortOrder.DATE_DESC.name)
                ?: FlashcardSortOrder.DATE_DESC.name
        )
    )
    val sortOrder: StateFlow<FlashcardSortOrder> = _sortOrder.asStateFlow()

    val totalFlashcards: StateFlow<Int> = flashcardDao.observeCountByDeck(deckId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dueTodayCount: StateFlow<Int> = flashcardProgressDao.observeDueCount(deckId, LocalDate.now().toEpochDay())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val masteryPercentage: StateFlow<Float> = combine(
        totalFlashcards,
        flashcardProgressDao.observeProgressByDeck(deckId)
    ) { total, progressList ->
        if (total == 0) return@combine 0f

        // Fiszka uznana za "opanowaną" jeśli interwał >= 21 dni
        val masteredCount = progressList.count { it.intervalDays >= 21 }

        (masteredCount.toFloat() / total.toFloat()) * 100f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val sortedFlashcards: StateFlow<List<Flashcard>> = combine(
        flashcardDao.observeByDeck(deckId),
        flashcardProgressDao.observeProgressByDeck(deckId),
        _sortOrder
    ) { cards, progressList, order ->
        val progressMap = progressList.associateBy { it.flashcardId }

        when (order) {
            FlashcardSortOrder.DATE_DESC -> cards.sortedByDescending { it.id }
            FlashcardSortOrder.ALPHABETICAL -> cards.sortedBy { it.question.lowercase() }
            FlashcardSortOrder.DIFFICULTY_ASC -> cards.sortedBy { card ->
                progressMap[card.id]?.easeFactor ?: 2.5
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadDeck()
    }

    fun updateSortOrder(newOrder: FlashcardSortOrder) {
        prefs.edit().putString(SORT_PREFS_KEY, newOrder.name).apply()
        _sortOrder.value = newOrder
    }

    private fun loadDeck() {
        viewModelScope.launch {
            deckDao.observeAllWithCount().map { list ->
                list.find { it.id == deckId }
            }.collect {
                _deck.value = it
            }
        }
    }
}