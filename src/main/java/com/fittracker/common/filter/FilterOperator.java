package com.fittracker.common.filter;

public enum FilterOperator {
  EQ,
  NEQ,
  GT,
  GTE,
  LT,
  LTE,
  IN,
  LIKE;

  public static FilterOperator from(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("Operateur null");
    }
    try {
      return FilterOperator.valueOf(raw.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Operateur invalide '" + raw + "'", ex);
    }
  }
}
