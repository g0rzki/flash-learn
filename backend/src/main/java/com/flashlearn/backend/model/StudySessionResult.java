package com.flashlearn.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_session_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudySessionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;

    // 0 = nie wiem, 1 = trudne, 2 = łatwe
    @Column(nullable = false)
    private int rating;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        if (this.reviewedAt == null) {
            this.reviewedAt = LocalDateTime.now();
        }
    }
}
