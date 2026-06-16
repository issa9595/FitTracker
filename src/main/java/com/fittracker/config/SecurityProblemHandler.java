package com.fittracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Uniformise les rejets de Spring Security (401 sans/avec jeton invalide, 403 acces refuse) au meme
 * format RFC 7807 {@code application/problem+json} que {@link
 * com.fittracker.common.error.ApiErrorHandler}, pour un contrat d'erreur coherent sur toute l'API.
 */
@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private static final String BASE_TYPE = "https://fittracker.dev/problems/";

  private final ObjectMapper objectMapper;

  public SecurityProblemHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    write(request, response, HttpStatus.UNAUTHORIZED, "Non authentifie", "unauthorized",
        "Authentification requise");
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    write(request, response, HttpStatus.FORBIDDEN, "Acces refuse", "forbidden",
        "Acces a la ressource refuse");
  }

  private void write(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String title,
      String slug,
      String detail)
      throws IOException {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(URI.create(BASE_TYPE + slug));
    pd.setTitle(title);
    pd.setInstance(URI.create(request.getRequestURI()));
    pd.setProperty("timestamp", OffsetDateTime.now().toString());
    String traceId = MDC.get("traceId");
    if (traceId != null) {
      pd.setProperty("traceId", traceId);
    }
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), pd);
  }
}
