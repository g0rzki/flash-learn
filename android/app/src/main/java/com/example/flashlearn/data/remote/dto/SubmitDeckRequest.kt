package com.example.flashlearn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Ciało żądania POST /marketplace/submit.
 * deckId      – lokalny serverId talii (Long z Room → wysyłany do backendu)
 * categoryId  – id kategorii z GET /categories
 * description – opcjonalny opis widoczny w Marketplace
 */
data class SubmitDeckRequest(
    @SerializedName("deckId")      val deckId: Long,
    @SerializedName("categoryId")  val categoryId: Long,
    @SerializedName("description") val description: String?
)
