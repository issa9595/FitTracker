package com.fittracker.training.dto;

import com.fittracker.training.SessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

@Schema(description = "Mise a jour partielle d'une session")
public record TrainingSessionUpdateRequest(
    OffsetDateTime startedAt,
    @Min(0) Integer durationSeconds,
    SessionType type,
    @Size(max = 500) String notes) {}
