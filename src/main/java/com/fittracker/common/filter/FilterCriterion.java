package com.fittracker.common.filter;

import java.util.List;

public record FilterCriterion(String field, FilterOperator operator, List<String> values) {

  public String single() {
    if (values.isEmpty()) {
      throw new IllegalArgumentException("Aucune valeur pour le filtre '" + field + "'");
    }
    return values.get(0);
  }
}
