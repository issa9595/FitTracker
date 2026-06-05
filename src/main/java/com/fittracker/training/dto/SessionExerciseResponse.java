package com.fittracker.training.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Exercice attache a une session")
public record SessionExerciseResponse(
    UUID exerciseId,
    int position,
    Integer sets,
    Integer reps,
    Double weightKg,
    Integer distanceM,
    Integer timeSeconds) {}
