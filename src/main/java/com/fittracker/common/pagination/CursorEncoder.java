package com.fittracker.common.pagination;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Encode/decode des curseurs opaques en base64 URL-safe. Le curseur encapsule simplement la valeur
 * cle (typiquement l'ID UUID du dernier element). Les clients doivent traiter cette chaine comme
 * opaque : sa structure peut changer sans casser le contrat.
 */
@Component
public class CursorEncoder {

  public String encode(String rawValue) {
    if (rawValue == null) {
      return null;
    }
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(rawValue.getBytes(StandardCharsets.UTF_8));
  }

  public String decode(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Curseur invalide", ex);
    }
  }
}
