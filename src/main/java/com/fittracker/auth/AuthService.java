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

/**
 * Implementation stub pour la Phase 3 : valide les inputs et persiste un User, mais renvoie un
 * faux access token. La cryptographie reelle (BCrypt + JWT RS256) arrivera en Phase 6.
 */
@Service
public class AuthService {

  private static final String STUB_TOKEN = "stub-token-phase-6";
  private static final long STUB_EXPIRES_IN_SECONDS = 900L;

  private final UserRepository userRepository;

  public AuthService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public AuthResponse register(RegisterRequest req) {
    if (userRepository.existsByEmail(req.email())) {
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
    return new AuthResponse(
        user.getId(), user.getEmail(), STUB_TOKEN, "bearer", STUB_EXPIRES_IN_SECONDS);
  }

  public AuthResponse login(LoginRequest req) {
    User user =
        userRepository.findByEmail(req.email()).orElseThrow(() -> new NotFoundException("User", req.email()));
    return new AuthResponse(
        user.getId(), user.getEmail(), STUB_TOKEN, "bearer", STUB_EXPIRES_IN_SECONDS);
  }
}
