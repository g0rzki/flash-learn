package com.example.flashlearn.data.remote

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Path

interface FlashcardApiService {
    @DELETE("/decks/{deckId}/flashcards/{id}")
    suspend fun deleteFlashcard(
        @Path("deckId") deckId: Long,
        @Path("id") id: Long
    ): Response<Unit>
}
