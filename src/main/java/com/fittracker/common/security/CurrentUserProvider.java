package com.fittracker.common.security;

import com.fittracker.common.error.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Fournit l'identifiant de l'utilisateur courant.
 *
 * <p>Phase 6 : la source unique est le {@code SecurityContext} de Spring Security. Le filtre du
 * Resource Server valide le JWT (HS256) et place une {@code Authentication} dont le nom est le
 * {@code sub} du token (= userId). Une requete non authentifiee est rejetee en amont par la chaine
 * de filtres (401) ; l'absence de contexte ici leve une {@link UnauthorizedException} par securite.
 */
@Component
public class CurrentUserProvider {

  /** Utilisateur de demo seede en profils dev/test (cf. UserSeed). */
  public static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  public UUID currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      throw new UnauthorizedException("Authentification requise");
    }
    try {
      return UUID.fromString(auth.getName());
    } catch (IllegalArgumentException ex) {
      throw new UnauthorizedException("Sujet du jeton invalide");
    }
  }
}
