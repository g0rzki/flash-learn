package com.flashlearn.backend.session;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
@Tag(name = "Sesje nauki", description = "Zapis wyników sesji nauki i aktualizacja postępu SM-2")
@SecurityRequirement(name = "bearerAuth")
public class SessionController {

    private final SessionService sessionService;

    @Operation(summary = "Zapisz wyniki sesji nauki",
            description = "Zapisuje sesję nauki i aktualizuje postęp SM-2 dla każdej ocenionej fiszki.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sesja zapisana, postęp SM-2 zaktualizowany"),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "404", description = "Talia lub fiszka nie istnieje")
    })
    @PostMapping
    public ResponseEntity<SessionResponse> save(@Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.save(request));
    }
}