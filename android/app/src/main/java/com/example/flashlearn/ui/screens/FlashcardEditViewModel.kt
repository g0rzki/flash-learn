package com.example.flashlearn.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.data.repository.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FLASHCARD_FIELD_MAX = 500

data class FlashcardEditUiState(
    val question: String = "",
    val answer: String = "",
    val questionError: String? = null,
    val answerError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class FlashcardEditViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** null means "create new flashcard" */
    private val flashcardId: Long? = savedStateHandle.get<Long>("flashcardId")?.takeIf { it != -1L }

    /** Provided by nav arg when creating, resolved from DB when editing */
    private var resolvedDeckId: Long = savedStateHandle.get<Long>("deckId") ?: 0L

    private val _uiState = MutableStateFlow(FlashcardEditUiState(isLoading = flashcardId != null))
    val uiState: StateFlow<FlashcardEditUiState> = _uiState

    init {
        loadFlashcardIfEditing()
    }

    private fun loadFlashcardIfEditing() {
        val id = flashcardId ?: return
        viewModelScope.launch {
            val flashcard = flashcardRepository.getById(id)
            if (flashcard != null) {
                resolvedDeckId = flashcard.deckId
                _uiState.value = _uiState.value.copy(
                    question = flashcard.question,
                    answer = flashcard.answer,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onQuestionChange(value: String) {
        _uiState.value = _uiState.value.copy(
            question = value.take(FLASHCARD_FIELD_MAX),
            questionError = null
        )
    }

    fun onAnswerChange(value: String) {
        _uiState.value = _uiState.value.copy(
            answer = value.take(FLASHCARD_FIELD_MAX),
            answerError = null
        )
    }

    fun save(
        errQuestionRequired: String,
        errQuestionMax: String,
        errAnswerRequired: String,
        errAnswerMax: String
    ) {
        val state = _uiState.value
        val qError = when {
            state.question.isBlank()                  -> errQuestionRequired
            state.question.length > FLASHCARD_FIELD_MAX -> String.format(errQuestionMax, FLASHCARD_FIELD_MAX)
            else                                       -> null
        }
        val aError = when {
            state.answer.isBlank()                  -> errAnswerRequired
            state.answer.length > FLASHCARD_FIELD_MAX -> String.format(errAnswerMax, FLASHCARD_FIELD_MAX)
            else                                     -> null
        }
        if (qError != null || aError != null) {
            _uiState.value = state.copy(questionError = qError, answerError = aError)
            return
        }

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            flashcardRepository.saveFlashcard(
                flashcardId = flashcardId,
                deckId = resolvedDeckId,
                question = state.question.trim(),
                answer = state.answer.trim()
            )
            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
        }
    }

    companion object {
        const val FIELD_MAX = FLASHCARD_FIELD_MAX
    }
}
