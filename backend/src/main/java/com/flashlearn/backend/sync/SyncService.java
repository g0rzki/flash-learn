package com.flashlearn.backend.sync;

import com.flashlearn.backend.model.Deck;
import com.flashlearn.backend.model.Flashcard;
import com.flashlearn.backend.model.User;
import com.flashlearn.backend.repository.DeckRepository;
import com.flashlearn.backend.repository.FlashcardRepository;
import com.flashlearn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serwis obsługujący synchronizację danych z urządzenia mobilnego do serwera.
 * Strategia rozwiązywania konfliktów: server-wins — dane serwera mają priorytet
 * gdy obie strony zmodyfikowały ten sam rekord.
 */
@Service
@RequiredArgsConstructor
public class SyncService {

    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final UserRepository userRepository;

    /**
     * Przetwarza zmiany przesłane z urządzenia mobilnego.
     * Dla każdej talii i fiszki stosuje strategię server-wins przy konflikcie.
     *
     * @param request lista zmian z urządzenia wraz z timestamp klienta
     * @return podsumowanie przetworzonych zmian i lista konfliktów
     */
    @Transactional
    public SyncPushResponse push(SyncPushRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> conflicts = new ArrayList<>();
        int decksProcessed = 0;
        int flashcardsProcessed = 0;

        // Przetwarzanie talii
        if (request.getDecks() != null) {
            for (SyncDeckDTO dto : request.getDecks()) {
                String conflict = processDeck(dto, user, request.getClientTimestamp());
                if (conflict != null) {
                    conflicts.add(conflict);
                }
                decksProcessed++;
            }
        }

        // Przetwarzanie fiszek
        if (request.getFlashcards() != null) {
            for (SyncFlashcardDTO dto : request.getFlashcards()) {
                String conflict = processFlashcard(dto, request.getClientTimestamp());
                if (conflict != null) {
                    conflicts.add(conflict);
                }
                flashcardsProcessed++;
            }
        }

        return new SyncPushResponse(decksProcessed, flashcardsProcessed, conflicts, LocalDateTime.now());
    }

    /**
     * Przetwarza pojedynczą talię — tworzy nową lub aktualizuje istniejącą.
     * Przy konflikcie (obie strony edytowały) wygrywa serwer.
     *
     * @return opis konfliktu lub null jeśli brak konfliktu
     */
    private String processDeck(SyncDeckDTO dto, User owner, LocalDateTime clientTimestamp) {
        if (dto.getId() == null) {
            // Nowa talia — zapisz
            Deck deck = Deck.builder()
                    .owner(owner)
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .isPublic(dto.isPublic())
                    .build();
            deckRepository.save(deck);
            return null;
        }

        // Istniejąca talia — sprawdź konflikt
        return deckRepository.findById(dto.getId()).map(existing -> {
            if (existing.getUpdatedAt() != null &&
                    existing.getUpdatedAt().isAfter(clientTimestamp)) {
                // Konflikt — serwer był edytowany po ostatnim sync klienta — server-wins
                return "Deck conflict (server-wins): id=" + dto.getId();
            }
            // Brak konfliktu — aktualizuj
            existing.setTitle(dto.getTitle());
            existing.setDescription(dto.getDescription());
            existing.setPublic(dto.isPublic());
            deckRepository.save(existing);
            return null;
        }).orElse(null);
    }

    /**
     * Przetwarza pojedynczą fiszkę — tworzy nową lub aktualizuje istniejącą.
     * Przy konflikcie wygrywa serwer.
     *
     * @return opis konfliktu lub null jeśli brak konfliktu
     */
    private String processFlashcard(SyncFlashcardDTO dto, LocalDateTime clientTimestamp) {
        if (dto.getId() == null) {
            // Nowa fiszka bez powiązanej talii — pomijamy (talia musi być najpierw zsynchronizowana)
            return null;
        }

        return flashcardRepository.findById(dto.getId()).map(existing -> {
            if (existing.getUpdatedAt() != null &&
                    existing.getUpdatedAt().isAfter(clientTimestamp)) {
                // Konflikt — server-wins
                return "Flashcard conflict (server-wins): id=" + dto.getId();
            }
            existing.setQuestion(dto.getQuestion());
            existing.setAnswer(dto.getAnswer());
            flashcardRepository.save(existing);
            return null;
        }).orElse(null);
    }
}