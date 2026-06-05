package com.fittracker.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Mise a jour des champs editables du user")
public record UserUpdateRequest(
    @Schema(example = "Jane Doe") @Size(min = 1, max = 80) String displayName) {}
