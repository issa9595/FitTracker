package com.fittracker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Inscription d'un nouvel utilisateur")
public record RegisterRequest(
    @Schema(example = "jane@example.com") @NotBlank @Email String email,
    @Schema(example = "ChangeMe123!") @NotBlank @Size(min = 8, max = 200) String password,
    @Schema(example = "Jane Doe") @NotBlank @Size(min = 1, max = 80) String displayName) {}
