package com.fittracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting des endpoints d'authentification (OWASP A07) : 60 requetes/minute par IP sur {@code
 * /api/v1/auth/**}, via un token bucket Bucket4j en memoire ({@code ConcurrentHashMap}). Au
 * depassement, repond 429 {@code application/problem+json}.
 *
 * <p>Mono-instance : le compteur est local au process. La version distribuee multi-instances
 * passerait par {@code bucket4j-redis} (Redis deja dans la stack), cf. docs/security.md.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";
  private static final int CAPACITY_PER_MINUTE = 60;

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper;

  public RateLimitFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(AUTH_PATH_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Bucket bucket = buckets.computeIfAbsent(clientIp(request), key -> newBucket());
    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
    } else {
      writeTooManyRequests(request, response);
    }
  }

  private Bucket newBucket() {
    Bandwidth limit =
        Bandwidth.classic(
            CAPACITY_PER_MINUTE, Refill.greedy(CAPACITY_PER_MINUTE, Duration.ofMinutes(1)));
    return Bucket.builder().addLimit(limit).build();
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader(X_FORWARDED_FOR);
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS, "Trop de requetes, reessayez plus tard");
    pd.setType(URI.create("https://fittracker.dev/problems/rate-limit"));
    pd.setTitle("Limite de debit atteinte");
    pd.setInstance(URI.create(request.getRequestURI()));
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), pd);
  }
}
