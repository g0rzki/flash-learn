package com.flashlearn.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Postęp użytkownika w nauce konkretnej fiszki.
 * Przechowuje dane algorytmu SM-2 (spaced repetition).
 *
 * <p>Algorytm SM-2 oblicza optymalny odstęp między powtórkami
 * na podstawie oceny użytkownika (Nie wiem / Trudne / Łatwe).</p>
 *
 * @see <a href="https://www.supermemo.com/en/archives1990-2015/english/ol/sm2">SM-2 Algorithm</a>
 */
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

    /**
     * Współczynnik łatwości (Ease Factor) - im wyższy, tym dłuższe przerwy.
     * Wartość domyślna SM-2: 2.5. Minimum: 1.3.
     */
    @Column(name = "ease_factor", nullable = false)
    private double easeFactor = 2.5;

    /**
     * Aktualny interwał w dniach do następnej powtórki.
     */
    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 1;

    /**
     * Liczba udanych powtórek z rzędu.
     * Reset do 0 przy odpowiedzi "Nie wiem".
     */
    @Column(name = "repetitions", nullable = false)
    private int repetitions = 0;

    /**
     * Data następnej zaplanowanej powtórki.
     */
    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    @PrePersist
    protected void onCreate() {
        if (this.nextReviewDate == null) {
            this.nextReviewDate = LocalDate.now();
        }
    }
}
