package com.fittracker.common.security;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Fournit l'identifiant de l'utilisateur courant.
 *
 * <p>Phase 5 : si une requete porte un JWT valide, {@link com.fittracker.auth.JwtAuthFilter} place
 * l'identifiant correspondant dans un contexte par thread. Sinon, on retombe sur un utilisateur de
 * test fixe pour conserver le comportement des phases precedentes (et des tests). En Phase 6, la
 * source unique deviendra le SecurityContext de Spring Security.
 */
@Component
public class CurrentUserProvider {

  public static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

  public void set(UUID userId) {
    CURRENT.set(userId);
  }

  public void clear() {
    CURRENT.remove();
  }

  public UUID currentUserId() {
    UUID userId = CURRENT.get();
    return userId != null ? userId : TEST_USER_ID;
  }
}
