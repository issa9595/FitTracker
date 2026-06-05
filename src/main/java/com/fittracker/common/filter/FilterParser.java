package com.fittracker.common.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Parser du mini-DSL de filtrage : ?filter=field:op:value,field:op:value2,...
 *
 * <p>Les valeurs IN sont multiples, separees par '|' (ex. type:IN:RUNNING|MMA). Les valeurs ne
 * peuvent pas contenir ':' ou ',' ou '|' (encoder cote client si besoin).
 */
@Component
public class FilterParser {

  public FilterSpec parse(String filter, String filterOp) {
    FilterCombinator combinator = FilterCombinator.from(filterOp);
    if (filter == null || filter.isBlank()) {
      return new FilterSpec(List.of(), combinator);
    }
    List<FilterCriterion> criteria = new ArrayList<>();
    for (String token : filter.split(",")) {
      if (token.isBlank()) {
        continue;
      }
      String[] parts = token.split(":", 3);
      if (parts.length != 3) {
        throw new IllegalArgumentException(
            "Filtre invalide '" + token + "', attendu 'field:op:value'");
      }
      String field = parts[0].trim();
      FilterOperator op = FilterOperator.from(parts[1].trim());
      String rawValue = parts[2];
      List<String> values =
          op == FilterOperator.IN ? Arrays.asList(rawValue.split("\\|")) : List.of(rawValue);
      criteria.add(new FilterCriterion(field, op, values));
    }
    return new FilterSpec(criteria, combinator);
  }
}
