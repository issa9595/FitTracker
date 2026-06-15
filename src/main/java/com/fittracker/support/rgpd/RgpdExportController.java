package com.fittracker.support.rgpd;

import com.fittracker.common.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint RGPD de portabilite : export complet des donnees de l'utilisateur courant. */
@RestController
@RequestMapping(value = "/api/v1/users/me/export", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "RGPD", description = "Droit a la portabilite et a l'effacement des donnees")
public class RgpdExportController {

  private final RgpdExportService service;
  private final CurrentUserProvider currentUser;

  public RgpdExportController(RgpdExportService service, CurrentUserProvider currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @Operation(summary = "Exporte toutes les donnees de l'utilisateur courant (RGPD portabilite)")
  @GetMapping
  public ResponseEntity<Map<String, Object>> export() {
    return ResponseEntity.ok(service.exportFor(currentUser.currentUserId()));
  }
}
