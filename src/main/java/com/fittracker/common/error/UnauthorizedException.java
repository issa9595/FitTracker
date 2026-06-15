package com.fittracker.common.error;

/**
 * Levee quand une requete protegee n'est pas (ou mal) authentifiee au niveau applicatif : mauvais
 * identifiants au login, ou {@code SecurityContext} sans authentification valide. Mappee en HTTP 401
 * (RFC 7807) par {@link ApiErrorHandler}.
 */
public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(String message) {
    super(message);
  }
}
