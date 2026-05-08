package com.flashlearn.backend.marketplace;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MarketplaceDeckResponse {

    private Long id;
    private String title;
    private String description;
    private String ownerEmail;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String categoryIconName;
    private int flashcardCount;
    private long downloadCount;
    private LocalDateTime createdAt;
}