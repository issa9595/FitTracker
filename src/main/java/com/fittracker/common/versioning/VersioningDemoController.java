package com.fittracker.common.versioning;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demonstrateur de versioning par negociation de contenu.
 *
 * <p>La strategie principale du projet est le prefixe URI (/api/v1/...). Cet endpoint montre la
 * strategie secondaire : meme URI, mais reponse different selon l'en-tete Accept.
 *
 * <ul>
 *   <li><code>Accept: application/json</code> ou absent -> reponse v1 + headers Deprecation/Sunset/Link
 *   <li><code>Accept: application/vnd.fittracker.v2+json</code> -> reponse v2 enrichie
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/version-demo/widgets")
@Tag(
    name = "Versioning demo",
    description = "Demo de negociation de contenu Accept v1 vs v2 + headers Deprecation/Sunset")
public class VersioningDemoController {

  private static final String V2_MEDIA_TYPE = "application/vnd.fittracker.v2+json";

  @Operation(summary = "Renvoie une liste de widgets en format v1 (deprecie pour demo)")
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<Map<String, Object>>> listV1() {
    List<Map<String, Object>> body =
        List.of(
            Map.of("id", "w1", "name", "Widget A"),
            Map.of("id", "w2", "name", "Widget B"));
    HttpHeaders headers = new HttpHeaders();
    headers.add("Deprecation", "true");
    headers.add("Sunset", "Wed, 16 Jun 2027 00:00:00 GMT");
    headers.add("Link", "</api/v1/version-demo/widgets>; rel=\"successor-version\"; type=\"" + V2_MEDIA_TYPE + "\"");
    return ResponseEntity.ok().headers(headers).body(body);
  }

  @Operation(
      summary = "Renvoie une liste de widgets en format v2 (Accept: application/vnd.fittracker.v2+json)")
  @GetMapping(produces = V2_MEDIA_TYPE)
  public Map<String, Object> listV2() {
    return Map.of(
        "version", "v2",
        "items",
        List.of(
            Map.of("id", "w1", "name", "Widget A", "tags", List.of("alpha", "beta")),
            Map.of("id", "w2", "name", "Widget B", "tags", List.of("beta"))),
        "count", 2);
  }
}
