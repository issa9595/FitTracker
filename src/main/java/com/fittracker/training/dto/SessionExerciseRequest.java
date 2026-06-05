package com.fittracker.training.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Ajout d'un exercice dans une session existante")
public record SessionExerciseRequest(
    @Schema(example = "00000000-0000-0000-0000-0000000000b1") @NotNull UUID exerciseId,
    @Schema(example = "1") @Min(0) Integer position,
    @Schema(example = "4") @Min(0) Integer sets,
    @Schema(example = "10") @Min(0) Integer reps,
    @Schema(example = "80.0") @Min(0) Double weightKg,
    @Schema(example = "5000") @Min(0) Integer distanceM,
    @Schema(example = "1800") @Min(0) Integer timeSeconds) {}
