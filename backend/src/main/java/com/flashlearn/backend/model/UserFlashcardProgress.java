package com.flashlearn.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "user_flashcard_progress",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "flashcard_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserFlashcardProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;

    // SM-2 algorithm fields
    @Column(name = "ease_factor", nullable = false)
    private double easeFactor = 2.5;  // default EF in SM-2

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 1;

    @Column(name = "repetitions", nullable = false)
    private int repetitions = 0;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    @PrePersist
    protected void onCreate() {
        if (this.nextReviewDate == null) {
            this.nextReviewDate = LocalDate.now();
        }
    }
}
