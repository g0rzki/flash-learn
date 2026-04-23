package com.flashlearn.backend.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SessionRequest {

    @NotNull(message = "Deck ID is required")
    private Long deckId;

    @NotNull(message = "Started at is required")
    private LocalDateTime startedAt;

    @NotNull(message = "Finished at is required")
    private LocalDateTime finishedAt;

    @NotEmpty(message = "Results cannot be empty")
    @Valid
    private List<SessionResultRequest> results;
}