package com.example.flashlearn.data.remote.dto

/**
 * Mapuje CloneResponse z backendu (201 Created).
 */
data class CloneResponseDto(
    val deckId: Long,
    val title: String,
    val description: String?,
    val flashcards: List<ClonedFlashcardDto>
)

data class ClonedFlashcardDto(
    val id: Long,
    val question: String,
    val answer: String
)
