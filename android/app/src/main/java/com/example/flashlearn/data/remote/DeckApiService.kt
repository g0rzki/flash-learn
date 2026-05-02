package com.example.flashlearn.data.remote

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Path

interface DeckApiService {
    @DELETE("/decks/{id}")
    suspend fun deleteDeck(@Path("id") id: Long): Response<Unit>
}
