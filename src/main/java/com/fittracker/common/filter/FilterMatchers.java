package com.fittracker.common.filter;

import java.util.function.Function;
import java.util.function.Predicate;

/** Helpers de traduction FilterCriterion -> Predicate pour les filtres in-memory de la Phase 3. */
public final class FilterMatchers {

  private FilterMatchers() {}

  /** Filtre sur un champ texte. */
  public static <T> Predicate<T> string(Function<T, String> extractor, FilterCriterion c) {
    String expected = c.single();
    return switch (c.operator()) {
      case EQ -> t -> expected.equals(extractor.apply(t));
      case NEQ -> t -> !expected.equals(extractor.apply(t));
      case LIKE -> t -> {
        String v = extractor.apply(t);
        return v != null && v.toLowerCase().contains(expected.toLowerCase());
      };
      case IN -> t -> c.values().contains(extractor.apply(t));
      default ->
          throw new IllegalArgumentException(
              "Operateur '" + c.operator() + "' non supporte pour le champ texte '" + c.field() + "'");
    };
  }

  /** Filtre sur un champ numerique long. */
  public static <T> Predicate<T> longValue(Function<T, Long> extractor, FilterCriterion c) {
    long expected = Long.parseLong(c.single());
    return switch (c.operator()) {
      case EQ -> t -> expected == extractor.apply(t);
      case NEQ -> t -> expected != extractor.apply(t);
      case GT -> t -> extractor.apply(t) > expected;
      case GTE -> t -> extractor.apply(t) >= expected;
      case LT -> t -> extractor.apply(t) < expected;
      case LTE -> t -> extractor.apply(t) <= expected;
      default ->
          throw new IllegalArgumentException(
              "Operateur '" + c.operator() + "' non supporte pour un champ numerique");
    };
  }
}
