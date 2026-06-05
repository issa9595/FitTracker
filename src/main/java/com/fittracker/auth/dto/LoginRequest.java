package com.fittracker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Authentification email + mot de passe")
public record LoginRequest(
    @Schema(example = "jane@example.com") @NotBlank @Email String email,
    @Schema(example = "ChangeMe123!") @NotBlank String password) {}
