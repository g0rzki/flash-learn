package com.example.flashlearn.data.remote.dto

/**
 * Mapuje MarketplacePageResponse z backendu.
 */
data class MarketplacePageDto(
    val decks: List<MarketplaceDeckDto>,
    val page: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
)
