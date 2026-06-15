package com.fittracker.auth;

import com.fittracker.common.security.CurrentUserProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtre REST minimal (Phase 5) : si un header {@code Authorization: Bearer <jwt>} valide est
 * present, l'identifiant extrait alimente {@link CurrentUserProvider} le temps de la requete. En
 * l'absence de token, on laisse le fallback (utilisateur de test). La securisation complete
 * (Spring Security Resource Server) arrive en Phase 6.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final CurrentUserProvider currentUserProvider;

  public JwtAuthFilter(JwtService jwtService, CurrentUserProvider currentUserProvider) {
    this.jwtService = jwtService;
    this.currentUserProvider = currentUserProvider;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    boolean set = false;
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      String token = header.substring(BEARER_PREFIX.length());
      var userId = jwtService.validateAndExtractUserId(token);
      if (userId.isPresent()) {
        currentUserProvider.set(userId.get());
        set = true;
      }
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      if (set) {
        currentUserProvider.clear();
      }
    }
  }
}
