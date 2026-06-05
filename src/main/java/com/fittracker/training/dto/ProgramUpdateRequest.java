package com.fittracker.training.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Mise a jour partielle d'un programme")
public record ProgramUpdateRequest(
    @Size(max = 120) String name,
    @Size(max = 1000) String description,
    @Size(max = 120) String targetMetric,
    LocalDate startDate,
    LocalDate endDate) {}
