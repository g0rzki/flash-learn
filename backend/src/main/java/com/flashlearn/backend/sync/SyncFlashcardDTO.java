package com.flashlearn.backend.sync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SyncFlashcardDTO {

    private Long id; // null jeśli nowa fiszka

    private Long localId; // id używane lokalnie w aplikacji

    private Long deckId; // id talii nadrzędnej (z serwera)

    @NotBlank(message = "Question is required")
    private String question;

    @NotBlank(message = "Answer is required")
    private String answer;

    @NotNull(message = "UpdatedAt is required")
    private LocalDateTime updatedAt;
}