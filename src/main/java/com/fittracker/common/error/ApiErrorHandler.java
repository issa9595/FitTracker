package com.fittracker.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Handler global d'exceptions conforme RFC 7807 (application/problem+json).
 *
 * <p>Toutes les erreurs sont serialisees avec les champs standards type/title/status/detail/instance,
 * plus extensions traceId et timestamp. Les erreurs de validation incluent un champ errors[].
 */
@RestControllerAdvice
public class ApiErrorHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiErrorHandler.class);
  private static final String BASE_TYPE = "https://fittracker.dev/problems/";

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex, HttpServletRequest req) {
    var pd = build(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage(), req, "not-found");
    pd.setProperty("resource", ex.getResource());
    pd.setProperty("identifier", String.valueOf(ex.getIdentifier()));
    return respond(pd);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex, HttpServletRequest req) {
    return respond(build(HttpStatus.CONFLICT, "Conflit", ex.getMessage(), req, "conflict"));
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ProblemDetail> handleBusiness(BusinessRuleException ex, HttpServletRequest req) {
    return respond(
        build(HttpStatus.UNPROCESSABLE_ENTITY, "Regle metier violee", ex.getMessage(), req, "business-rule"));
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ProblemDetail> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
    return respond(build(HttpStatus.FORBIDDEN, "Acces refuse", ex.getMessage(), req, "forbidden"));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ProblemDetail> handleUnauthorized(
      UnauthorizedException ex, HttpServletRequest req) {
    return respond(build(HttpStatus.UNAUTHORIZED, "Non authentifie", ex.getMessage(), req, "unauthorized"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    List<Map<String, Object>> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldErrorMap)
            .toList();
    var pd =
        build(
            HttpStatus.BAD_REQUEST,
            "Donnees invalides",
            "La requete contient des champs invalides",
            req,
            "validation");
    pd.setProperty("errors", errors);
    return respond(pd);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
    var pd =
        build(
            HttpStatus.BAD_REQUEST,
            "Parametre invalide",
            "Le parametre '%s' a un format invalide".formatted(ex.getName()),
            req,
            "type-mismatch");
    pd.setProperty("parameter", ex.getName());
    pd.setProperty("expectedType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "?");
    return respond(pd);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleUnreadable(
      HttpMessageNotReadableException ex, HttpServletRequest req) {
    return respond(
        build(
            HttpStatus.BAD_REQUEST,
            "Corps de requete invalide",
            "Le corps JSON est mal forme ou manquant",
            req,
            "malformed-body"));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> handleIllegalArg(
      IllegalArgumentException ex, HttpServletRequest req) {
    return respond(
        build(HttpStatus.BAD_REQUEST, "Requete invalide", ex.getMessage(), req, "bad-request"));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemDetail> handleNoResource(
      NoResourceFoundException ex, HttpServletRequest req) {
    // Chemin sans handler ni ressource statique (ex. "/") : 404, pas 500.
    return respond(
        build(
            HttpStatus.NOT_FOUND,
            "Ressource introuvable",
            "Aucune ressource pour " + req.getRequestURI(),
            req,
            "not-found"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest req) {
    LOG.error("Erreur non geree", ex);
    return respond(
        build(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Erreur interne",
            "Une erreur inattendue est survenue",
            req,
            "internal"));
  }

  private ProblemDetail build(
      HttpStatus status, String title, String detail, HttpServletRequest req, String slug) {
    var pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(URI.create(BASE_TYPE + slug));
    pd.setTitle(title);
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("timestamp", OffsetDateTime.now().toString());
    String traceId = MDC.get("traceId");
    if (traceId != null) {
      pd.setProperty("traceId", traceId);
    }
    return pd;
  }

  private Map<String, Object> toFieldErrorMap(FieldError fe) {
    return Map.of(
        "field", fe.getField(),
        "code", fe.getCode() == null ? "invalid" : fe.getCode(),
        "message", fe.getDefaultMessage() == null ? "valeur invalide" : fe.getDefaultMessage(),
        "rejectedValue", String.valueOf(fe.getRejectedValue()));
  }

  private ResponseEntity<ProblemDetail> respond(ProblemDetail pd) {
    return ResponseEntity.status(pd.getStatus())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(pd);
  }
}
