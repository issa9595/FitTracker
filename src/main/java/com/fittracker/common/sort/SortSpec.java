package com.fittracker.common.sort;

import java.util.Comparator;
import java.util.function.Function;

/**
 * Parse 'champ,direction' venant du parametre ?sort= et produit un Comparator. En Phase 4, ce
 * pattern sera remplace par org.springframework.data.domain.Sort.
 */
public record SortSpec(String field, Direction direction) {

  public enum Direction {
    ASC,
    DESC
  }

  public static SortSpec parse(String raw, String defaultField) {
    if (raw == null || raw.isBlank()) {
      return new SortSpec(defaultField, Direction.DESC);
    }
    String[] parts = raw.split(",", 2);
    String field = parts[0].trim();
    Direction direction =
        parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()) ? Direction.ASC : Direction.DESC;
    return new SortSpec(field, direction);
  }

  public <T, K extends Comparable<K>> Comparator<T> toComparator(Function<T, K> extractor) {
    Comparator<T> cmp = Comparator.comparing(extractor, Comparator.nullsLast(Comparator.naturalOrder()));
    return direction == Direction.DESC ? cmp.reversed() : cmp;
  }
}
