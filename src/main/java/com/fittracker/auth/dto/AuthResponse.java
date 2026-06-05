package com.fittracker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Reponse d'authentification (stub Phase 3, JWT reel en Phase 6)")
public record AuthResponse(
    @Schema(example = "00000000-0000-0000-0000-000000000001") UUID userId,
    @Schema(example = "jane@example.com") String email,
    @Schema(example = "stub-token-phase-6") String accessToken,
    @Schema(example = "bearer") String tokenType,
    @Schema(example = "900") long expiresInSeconds) {}
