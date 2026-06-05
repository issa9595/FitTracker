package com.fittracker.common.security;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Stub d'identite de l'utilisateur courant pour la Phase 3. En Phase 6, on lira l'identifiant
 * depuis le SecurityContext alimente par le JWT. Tant qu'on n'a pas l'auth complete, on renvoie
 * un user "test" fixe pour permettre aux endpoints /users/me et autorisations triviales de
 * fonctionner.
 */
@Component
public class CurrentUserProvider {

  public static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  public UUID currentUserId() {
    return TEST_USER_ID;
  }
}
