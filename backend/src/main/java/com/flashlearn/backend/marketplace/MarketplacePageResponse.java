package com.flashlearn.backend.marketplace;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MarketplacePageResponse {
    private List<MarketplaceDeckResponse> decks;
    private int page;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}