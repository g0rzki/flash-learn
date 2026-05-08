package com.flashlearn.backend.marketplace;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {

    @NotNull(message = "Deck ID is required")
    private Long deckId;

    private String reason;
}