package com.fittracker.auth;

import com.fittracker.auth.dto.AuthResponse;
import com.fittracker.auth.dto.LoginRequest;
import com.fittracker.auth.dto.RegisterRequest;
import com.fittracker.common.error.ConflictException;
import com.fittracker.common.error.NotFoundException;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 5 : emet de vrais access tokens JWT (HS256) via {@link JwtService}. La verification du mot
 * de passe (BCrypt) et les refresh tokens revocables arriveront en Phase 6.
 */
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final JwtService jwtService;

  public AuthService(UserRepository userRepository, JwtService jwtService) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
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
            "stub-hash-phase-6",
            req.displayName(),
            OffsetDateTime.now());
    userRepository.save(user);
    return toResponse(user);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest req) {
    User user =
        userRepository
            .findByEmailIgnoreCase(req.email())
            .orElseThrow(() -> new NotFoundException("User", req.email()));
    return toResponse(user);
  }

  private AuthResponse toResponse(User user) {
    String token = jwtService.generateAccessToken(user.getId(), user.getEmail());
    return new AuthResponse(
        user.getId(), user.getEmail(), token, "bearer", jwtService.getAccessTtlSeconds());
  }
}
