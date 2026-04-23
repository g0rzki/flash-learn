package com.flashlearn.backend.session;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SessionResponse {

    private Long sessionId;
    private Long deckId;
    private int cardsReviewed;
    private int correctAnswers;   // rating == 2 (łatwe)
    private int hardAnswers;      // rating == 1 (trudne)
    private int wrongAnswers;     // rating == 0 (nie wiem)
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}