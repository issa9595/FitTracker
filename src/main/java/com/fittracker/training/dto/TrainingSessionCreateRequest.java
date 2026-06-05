package com.fittracker.training.dto;

import com.fittracker.training.SessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

@Schema(description = "Creation d'une session d'entrainement")
public record TrainingSessionCreateRequest(
    @Schema(example = "2026-06-04T10:00:00Z") @NotNull OffsetDateTime startedAt,
    @Schema(example = "3600") @Min(0) int durationSeconds,
    @Schema(example = "STRENGTH") @NotNull SessionType type,
    @Schema(example = "Push day") @Size(max = 500) String notes) {}
