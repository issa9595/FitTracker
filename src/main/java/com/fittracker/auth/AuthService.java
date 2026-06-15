package com.fittracker.auth;

import com.fittracker.auth.dto.AuthResponse;
import com.fittracker.auth.dto.LoginRequest;
import com.fittracker.auth.dto.RegisterRequest;
import com.fittracker.common.error.ConflictException;
import com.fittracker.common.error.UnauthorizedException;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 6 : inscription avec hachage BCrypt (cost 12) et login verifiant le mot de passe. Emet de
 * vrais access tokens JWT (HS256) via {@link JwtService}, valides ensuite par le Resource Server.
 */
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public AuthResponse register(RegisterRequest req) {
    if (userRepository.existsByEmailIgnoreCase(req.email())) {
      throw new ConflictException("Email deja utilise");
    }
    User user =
        new User(
            UUID.randomUUID(),
            req.email(),
            passwordEncoder.encode(req.password()),
            req.displayName(),
            OffsetDateTime.now());
    userRepository.save(user);
    return toResponse(user);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest req) {
    // Message generique (pas de distinction "email inconnu" / "mauvais mot de passe") pour eviter
    // l'enumeration de comptes (OWASP A07).
    User user =
        userRepository
            .findByEmailIgnoreCase(req.email())
            .orElseThrow(() -> new UnauthorizedException("Identifiants invalides"));
    if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
      throw new UnauthorizedException("Identifiants invalides");
    }
    return toResponse(user);
  }

  private AuthResponse toResponse(User user) {
    String token = jwtService.generateAccessToken(user.getId(), user.getEmail());
    return new AuthResponse(
        user.getId(), user.getEmail(), token, "bearer", jwtService.getAccessTtlSeconds());
  }
}
