package com.flashlearn.backend.session;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionResultRequest {

    @NotNull(message = "Flashcard ID is required")
    private Long flashcardId;

    /**
     * Ocena fiszki: 0 = nie wiem, 1 = trudne, 2 = łatwe
     */
    @Min(value = 0, message = "Rating must be 0, 1 or 2")
    @Max(value = 2, message = "Rating must be 0, 1 or 2")
    private int rating;
}