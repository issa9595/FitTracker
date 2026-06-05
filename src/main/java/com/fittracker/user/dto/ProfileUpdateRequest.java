package com.fittracker.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "Mise a jour du profil sportif")
public record ProfileUpdateRequest(
    @Schema(example = "180") @Min(50) @Max(250) Integer heightCm,
    @Schema(example = "75.5") @Min(20) @Max(400) Double weightKg,
    @Schema(example = "72.0") @Min(20) @Max(400) Double goalWeightKg,
    @Schema(example = "Athlete amateur") @Size(max = 500) String bio) {}
