package com.fittracker.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Parametres de pagination offset/limit. Convention : page commence a 0,
 * sort = champ,direction (ex. "createdAt,desc"). Bornes par defaut : page=0, size=20, max size=100.
 */
@Schema(description = "Parametres de pagination offset/limit")
public record PageRequest(
    @Schema(description = "Index de la page (zero-based)", example = "0") int page,
    @Schema(description = "Nombre d'elements par page", example = "20") int size,
    @Schema(description = "Tri 'champ,direction' (ex. 'createdAt,desc')", example = "createdAt,desc")
        String sort) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  public PageRequest {
    if (page < 0) {
      throw new IllegalArgumentException("page doit etre >= 0");
    }
    if (size <= 0) {
      size = DEFAULT_SIZE;
    }
    if (size > MAX_SIZE) {
      size = MAX_SIZE;
    }
  }

  public static PageRequest of(int page, int size, String sort) {
    return new PageRequest(page, size, sort);
  }

  public int offset() {
    return page * size;
  }
}
