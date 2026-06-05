package com.fittracker.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Demande de suivi : followeeId est l'utilisateur a suivre")
public record FollowRequest(@NotNull UUID followeeId) {}
