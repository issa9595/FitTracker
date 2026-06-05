package com.fittracker.training.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Creation d'un programme d'entrainement")
public record ProgramCreateRequest(
    @Schema(example = "Prep semi-marathon") @NotBlank @Size(max = 120) String name,
    @Schema(example = "Programme 12 semaines") @Size(max = 1000) String description,
    @Schema(example = "10km en 50min") @Size(max = 120) String targetMetric,
    @Schema(example = "2026-06-15") @NotNull LocalDate startDate,
    @Schema(example = "2026-09-15") @NotNull LocalDate endDate) {}
