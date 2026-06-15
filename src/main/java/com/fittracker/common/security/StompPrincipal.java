package com.fittracker.common.security;

import java.security.Principal;

/** Principal minimal pour les sessions STOMP : porte l'identifiant utilisateur extrait du JWT. */
public record StompPrincipal(String name) implements Principal {

  @Override
  public String getName() {
    return name;
  }
}
