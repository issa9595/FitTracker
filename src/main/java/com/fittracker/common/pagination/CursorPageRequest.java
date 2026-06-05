package com.fittracker.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

/** Parametres de pagination par curseur opaque (recommande pour flux a fort volume). */
@Schema(description = "Pagination par curseur opaque")
public record CursorPageRequest(
    @Schema(description = "Curseur opaque renvoye par la requete precedente", example = "eyJpZCI6IjEyMyJ9")
        String cursor,
    @Schema(description = "Nombre d'elements par page", example = "20") int size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  public CursorPageRequest {
    if (size <= 0) {
      size = DEFAULT_SIZE;
    }
    if (size > MAX_SIZE) {
      size = MAX_SIZE;
    }
  }

  public static CursorPageRequest of(String cursor, int size) {
    return new CursorPageRequest(cursor, size);
  }
}
