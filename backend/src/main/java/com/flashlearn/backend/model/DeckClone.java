package com.flashlearn.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deck_clones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeckClone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_deck_id", nullable = false)
    private Deck originalDeck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloned_deck_id", nullable = false)
    private Deck clonedDeck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloned_by", nullable = false)
    private User clonedBy;

    @Column(name = "cloned_at", nullable = false, updatable = false)
    private LocalDateTime clonedAt;

    @PrePersist
    protected void onCreate() {
        this.clonedAt = LocalDateTime.now();
    }
}
