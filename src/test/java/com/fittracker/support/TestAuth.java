package com.fittracker.support;

import com.fittracker.auth.JwtService;
import com.fittracker.common.security.CurrentUserProvider;
import java.util.UUID;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Support de test : genere un vrai access token JWT (via {@link JwtService}, donc valide par le
 * Resource Server) et l'attache aux requetes MockMvc sous forme d'en-tete {@code Authorization:
 * Bearer ...}. A utiliser conjointement avec {@code .apply(springSecurity())} sur le MockMvc.
 */
public final class TestAuth {

  private TestAuth() {}

  /** PostProcessor ajoutant un Bearer token pour le {@code userId} donne. */
  public static RequestPostProcessor bearer(JwtService jwtService, UUID userId) {
    String token = jwtService.generateAccessToken(userId, "test@fittracker.dev");
    return request -> {
      request.addHeader("Authorization", "Bearer " + token);
      return request;
    };
  }

  /** PostProcessor ajoutant un Bearer token pour l'utilisateur de demo seede. */
  public static RequestPostProcessor bearerTestUser(JwtService jwtService) {
    return bearer(jwtService, CurrentUserProvider.TEST_USER_ID);
  }
}
