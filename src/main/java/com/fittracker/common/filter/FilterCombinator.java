package com.fittracker.common.filter;

public enum FilterCombinator {
  AND,
  OR;

  public static FilterCombinator from(String raw) {
    if (raw == null || raw.isBlank()) {
      return AND;
    }
    try {
      return FilterCombinator.valueOf(raw.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("filterOp doit etre AND ou OR", ex);
    }
  }
}
