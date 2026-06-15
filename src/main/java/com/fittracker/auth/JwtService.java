package com.fittracker.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service JWT minimal (HS256) pour la Phase 5 : signe et valide des access tokens utilises au login
 * REST et au handshake WebSocket. Sera durci en RS256 + refresh tokens revocables en Phase 6.
 */
@Service
public class JwtService {

  private static final Logger LOG = LoggerFactory.getLogger(JwtService.class);

  private final SecretKey key;
  private final String issuer;
  private final long accessTtlSeconds;

  public JwtService(
      @Value("${fittracker.security.jwt.secret}") String secret,
      @Value("${fittracker.security.jwt.issuer:fittracker}") String issuer,
      @Value("${fittracker.security.jwt.access-token-ttl-seconds:900}") long accessTtlSeconds) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.issuer = issuer;
    this.accessTtlSeconds = accessTtlSeconds;
  }

  /**
   * Genere un access token signe HS256 portant l'identifiant et l'email de l'utilisateur.
   *
   * <p>L'algorithme est explicitement epingle a HS256 : sans cela, jjwt deduirait l'algorithme de la
   * longueur de la cle (un secret &ge; 48 octets donnerait HS384/HS512), ce qui ferait diverger la
   * signature du {@code JwtDecoder} Nimbus du Resource Server, lui fige en {@code MacAlgorithm.HS256}.
   */
  public String generateAccessToken(UUID userId, String email) {
    Instant now = Instant.now();
    return Jwts.builder()
        .issuer(issuer)
        .subject(userId.toString())
        .claim("email", email)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  /** Valide la signature et l'expiration du token et renvoie l'identifiant utilisateur. */
  public Optional<UUID> validateAndExtractUserId(String token) {
    try {
      String subject =
          Jwts.parser()
              .verifyWith(key)
              .requireIssuer(issuer)
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getSubject();
      return Optional.of(UUID.fromString(subject));
    } catch (JwtException | IllegalArgumentException ex) {
      LOG.debug("JWT invalide : {}", ex.getMessage());
      return Optional.empty();
    }
  }

  public long getAccessTtlSeconds() {
    return accessTtlSeconds;
  }
}
