package com.fittracker.auth;

import com.fittracker.auth.dto.AuthResponse;
import com.fittracker.auth.dto.LoginRequest;
import com.fittracker.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auth", description = "Authentification (stub Phase 3, implementation reelle en Phase 6)")
public class AuthController {

  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @Operation(
      summary = "Inscription",
      description =
          "Cree un nouvel utilisateur et renvoie un access token de stub. En Phase 6 :"
              + " hashing BCrypt + JWT RS256.")
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest body) {
    return ResponseEntity.status(201).body(service.register(body));
  }

  @Operation(summary = "Login", description = "Renvoie un access token de stub (Phase 3).")
  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest body) {
    return service.login(body);
  }
}
