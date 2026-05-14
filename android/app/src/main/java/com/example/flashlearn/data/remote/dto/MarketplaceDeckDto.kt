package com.example.flashlearn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Mapuje MarketplaceDeckResponse z backendu.
 * Pola snake_case → camelCase przez @SerializedName (Gson).
 */
data class MarketplaceDeckDto(
    val id: Long,
    val title: String,
    val description: String?,
    /** Email właściciela talii (pole ownerEmail w backendzie). */
    val ownerEmail: String,
    val categoryId: Long?,
    val categoryName: String?,
    val categorySlug: String?,
    /** Nazwa ikony Material zgodna z backendem (np. "language", "code"). */
    val categoryIconName: String?,
    val flashcardCount: Int,
    val downloadCount: Long,
    val createdAt: String?
)
