package com.flashlearn.backend.seeder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashlearn.backend.model.Category;
import com.flashlearn.backend.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Seeds the database with categories, a test user and public decks in Marketplace.
 * Runs only when the "dev" Spring profile is active.
 *
 * Flow:
 *  1. Seed categories (jeśli nie istnieją) bezpośrednio przez JPA.
 *  2. Zarejestruj i zaloguj użytkownika testowego.
 *  3. Utwórz talie przez POST /decks.
 *  4. Dodaj fiszki przez POST /decks/{id}/flashcards.
 *  5. Zgłoś talię do Marketplace przez POST /marketplace/submit
 *     (przypisuje kategorię i ustawia isPublic=true).
 */
@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${server.port:8080}")
    private int port;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final CategoryRepository categoryRepository;

    public DataSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== DataSeeder starting ===");
        seedCategories();
        seedUser("tester@test.com", "test1234", testDecks());
        log.info("=== DataSeeder finished ===");
    }

    // ── 1. Categories ──────────────────────────────────────────────────────────

    private void seedCategories() {
        List<SeedCategory> cats = List.of(
            new SeedCategory("Języki",           "jezyki",        "language"),
            new SeedCategory("Programowanie",    "programowanie", "code"),
            new SeedCategory("Matematyka",       "matematyka",    "calculate"),
            new SeedCategory("Nauki ścisłe",     "nauki-scisle",  "science"),
            new SeedCategory("Historia",         "historia",      "history_edu"),
            new SeedCategory("Inne",             "inne",          "more_horiz")
        );

        for (SeedCategory sc : cats) {
            if (categoryRepository.findBySlug(sc.slug()).isEmpty()) {
                Category cat = new Category();
                cat.setName(sc.name());
                cat.setSlug(sc.slug());
                cat.setIconName(sc.iconName());
                categoryRepository.save(cat);
                log.info("  created category '{}'", sc.slug());
            } else {
                log.info("  category '{}' already exists – skipping", sc.slug());
            }
        }
    }

    private Long categoryIdBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(Category::getId)
                .orElse(null);
    }

    // ── 2. User + decks ────────────────────────────────────────────────────────

    private void seedUser(String email, String password, List<SeedDeck> decks) {
        try {
            register(email, password);
        } catch (HttpClientErrorException.Conflict e) {
            log.info("User {} already exists – skipping", email);
            return;
        } catch (Exception e) {
            log.warn("Could not register {}: {}", email, e.getMessage());
            return;
        }

        String token;
        try {
            token = login(email, password);
        } catch (Exception e) {
            log.warn("Could not login {}: {}", email, e.getMessage());
            return;
        }

        for (SeedDeck deck : decks) {
            try {
                Long deckId = createDeck(token, deck.title(), deck.description());
                for (SeedCard card : deck.cards()) {
                    createFlashcard(token, deckId, card.question(), card.answer());
                }

                // Zgłoś do Marketplace z kategorią (auto-accept)
                Long catId = categoryIdBySlug(deck.categorySlug());
                if (catId != null) {
                    submitToMarketplace(token, deckId, catId, deck.description());
                    log.info("  created & published deck '{}' [{}] with {} cards",
                            deck.title(), deck.categorySlug(), deck.cards().size());
                } else {
                    log.warn("  deck '{}' created but category '{}' not found – not published",
                            deck.title(), deck.categorySlug());
                }
            } catch (Exception e) {
                log.warn("  failed deck '{}': {}", deck.title(), e.getMessage());
            }
        }
    }

    // ── HTTP helpers ───────────────────────────────────────────────────────────

    private void register(String email, String password) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        rest.exchange(
                baseUrl() + "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("email", email, "password", password), h),
                Void.class);
    }

    @SuppressWarnings("unchecked")
    private String login(String email, String password) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("email", email, "password", password), h),
                String.class);
        Map<String, Object> body = mapper.readValue(resp.getBody(), Map.class);
        return (String) body.get("accessToken");
    }

    @SuppressWarnings("unchecked")
    private Long createDeck(String token, String title, String description) throws Exception {
        HttpHeaders h = authHeaders(token);
        // isPublic=false — zostanie ustawione przez /marketplace/submit
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/decks",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("title", title, "description", description, "isPublic", false), h),
                String.class);
        Map<String, Object> body = mapper.readValue(resp.getBody(), Map.class);
        return ((Number) body.get("id")).longValue();
    }

    private void createFlashcard(String token, Long deckId, String question, String answer) {
        HttpHeaders h = authHeaders(token);
        rest.exchange(
                baseUrl() + "/decks/" + deckId + "/flashcards",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("question", question, "answer", answer), h),
                Void.class);
    }

    private void submitToMarketplace(String token, Long deckId, Long categoryId, String description) {
        HttpHeaders h = authHeaders(token);
        Map<String, Object> payload = Map.of(
                "deckId", deckId,
                "categoryId", categoryId,
                "description", description
        );
        rest.exchange(
                baseUrl() + "/marketplace/submit",
                HttpMethod.POST,
                new HttpEntity<>(payload, h),
                Void.class);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    // ── Seed data ──────────────────────────────────────────────────────────────

    private List<SeedDeck> testDecks() {
        return List.of(
            new SeedDeck(
                "Angielski – poziom B2",
                "Słownictwo na poziomie B2 – trudniejsze wyrazy",
                "jezyki",
                List.of(
                    new SeedCard("What does 'ubiquitous' mean?",
                                 "Present or found everywhere – wszechobecny"),
                    new SeedCard("What does 'eloquent' mean?",
                                 "Fluent and persuasive in speaking – elokwentny"),
                    new SeedCard("What does 'meticulous' mean?",
                                 "Showing great attention to detail; very careful and precise – drobiazgowy"),
                    new SeedCard("What does 'resilient' mean?",
                                 "Able to withstand or recover quickly from difficult conditions – odporny"),
                    new SeedCard("What does 'ambiguous' mean?",
                                 "Open to more than one interpretation – dwuznaczny"),
                    new SeedCard("What does 'diligent' mean?",
                                 "Having care and conscientiousness in one's work – pilny, staranny")
                )
            ),
            new SeedDeck(
                "Matematyka dyskretna",
                "Podstawy matematyki dyskretnej – grafy, relacje, kombinatoryka",
                "matematyka",
                List.of(
                    new SeedCard("Co to jest graf skierowany?",
                                 "Graf, w którym krawędzie mają określony kierunek (łuk)."),
                    new SeedCard("Co to jest drzewo rozpinające?",
                                 "Podgraf spójny i acykliczny zawierający wszystkie wierzchołki grafu."),
                    new SeedCard("Co to jest graf dwudzielny?",
                                 "Graf, którego wierzchołki można podzielić na dwa rozłączne zbiory tak, " +
                                 "aby krawędzie łączyły jedynie wierzchołki z różnych zbiorów."),
                    new SeedCard("Czym jest cykl Eulera?",
                                 "Cykl w grafie, który przechodzi przez każdą krawędź dokładnie jeden raz."),
                    new SeedCard("Co to jest relacja równoważności?",
                                 "Relacja, która jest jednocześnie zwrotna, symetryczna i przechodnia."),
                    new SeedCard("Suma stopni wierzchołków w grafie?",
                                 "Równa podwojonej liczbie krawędzi (Lemat o uściskach dłoni).")
                )
            ),
            new SeedDeck(
                "Algorytmy i struktury danych",
                "Sortowanie, złożoność obliczeniowa i podstawowe struktury",
                "programowanie",
                List.of(
                    new SeedCard("Złożoność QuickSort (średni przypadek)?", "O(n log n)"),
                    new SeedCard("Co to jest stos (stack)?",
                                 "Struktura LIFO – Last In First Out. Operacje: push i pop."),
                    new SeedCard("Co to jest kolejka (queue)?",
                                 "Struktura FIFO – First In First Out."),
                    new SeedCard("Na czym polega wyszukiwanie binarne?",
                                 "Wielokrotny podział posortowanej tablicy na połowy. Złożoność O(log n)."),
                    new SeedCard("Co to jest tablica mieszająca (Hash Table)?",
                                 "Struktura mapująca klucze na wartości przez funkcję haszującą. O(1) średnio."),
                    new SeedCard("Złożoność Merge Sort?",
                                 "O(n log n) we wszystkich przypadkach.")
                )
            ),
            new SeedDeck(
                "Systemy Operacyjne",
                "Podstawowe pojęcia z systemów operacyjnych",
                "nauki-scisle",
                List.of(
                    new SeedCard("Co to jest zakleszczenie (deadlock)?",
                                 "Stan, w którym procesy czekają na zasoby zajęte przez siebie nawzajem."),
                    new SeedCard("Czym różni się proces od wątku?",
                                 "Proces ma własną przestrzeń adresową. Wątki współdzielą przestrzeń procesu."),
                    new SeedCard("Co to jest stronicowanie pamięci (paging)?",
                                 "Dzielenie pamięci fizycznej na ramki i logicznej na strony o stałym rozmiarze."),
                    new SeedCard("Do czego służy semafor?",
                                 "Do synchronizacji procesów/wątków i wzajemnego wykluczania dostępu do zasobów."),
                    new SeedCard("Co to jest system plików?",
                                 "Struktura logiczna kontrolująca zapis i odczyt danych na nośniku."),
                    new SeedCard("Na czym polega wywłaszczanie (preemption)?",
                                 "Czasowe przerwanie przez SO wykonywania procesu i przekazanie CPU innemu.")
                )
            ),
            new SeedDeck(
                "Historia Polski – XX wiek",
                "Kluczowe wydarzenia historyczne XX wieku w Polsce",
                "historia",
                List.of(
                    new SeedCard("Kiedy Polska odzyskała niepodległość?",
                                 "11 listopada 1918 roku."),
                    new SeedCard("Co to był Cud nad Wisłą?",
                                 "Zwycięstwo wojsk polskich nad Armią Czerwoną w sierpniu 1920 roku."),
                    new SeedCard("Kiedy wybuchło Powstanie Warszawskie?",
                                 "1 sierpnia 1944 roku; trwało 63 dni."),
                    new SeedCard("Co oznacza skrót PRL?",
                                 "Polska Rzeczpospolita Ludowa – komunistyczne państwo polskie w latach 1952–1989."),
                    new SeedCard("Kiedy powstała Solidarność?",
                                 "31 sierpnia 1980 roku, po strajku w Stoczni Gdańskiej."),
                    new SeedCard("Kiedy Polska wstąpiła do Unii Europejskiej?",
                                 "1 maja 2004 roku.")
                )
            ),
            new SeedDeck(
                "Język Niemiecki – A2",
                "Podstawowe słownictwo i zwroty na poziomie A2",
                "jezyki",
                List.of(
                    new SeedCard("Was bedeutet 'Entschuldigung'?",
                                 "Przepraszam / Excuse me."),
                    new SeedCard("Was bedeutet 'Krankenhaus'?",
                                 "Szpital / Hospital."),
                    new SeedCard("Was bedeutet 'Bahnhof'?",
                                 "Dworzec kolejowy / Train station."),
                    new SeedCard("Was bedeutet 'Schlüssel'?",
                                 "Klucz / Key."),
                    new SeedCard("Was bedeutet 'Lebensmittel'?",
                                 "Artykuły spożywcze / Groceries."),
                    new SeedCard("Was bedeutet 'Reisepass'?",
                                 "Paszport / Passport.")
                )
            )
        );
    }

    // ── record helpers ─────────────────────────────────────────────────────────

    private record SeedDeck(String title, String description, String categorySlug, List<SeedCard> cards) {}
    private record SeedCard(String question, String answer) {}
    private record SeedCategory(String name, String slug, String iconName) {}
}


