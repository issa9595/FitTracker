package com.fittracker.common.filter;

import java.util.List;
import java.util.function.Predicate;

/**
 * Specification de filtre parsee depuis ?filter=...&filterOp=... .
 *
 * <p>En Phase 3, l'application aux donnees se fait via Java Predicate (in-memory). En Phase 4, on
 * branchera la traduction vers JPA Specifications sans changer cette API.
 */
public record FilterSpec(List<FilterCriterion> criteria, FilterCombinator combinator) {

  public boolean isEmpty() {
    return criteria == null || criteria.isEmpty();
  }

  /**
   * Compose un Predicate sur T en combinant chaque critere via un fournisseur fournissant le
   * Predicate par critere (typiquement via switch sur field).
   */
  public <T> Predicate<T> toPredicate(java.util.function.Function<FilterCriterion, Predicate<T>> resolver) {
    if (isEmpty()) {
      return t -> true;
    }
    Predicate<T> combined = combinator == FilterCombinator.OR ? t -> false : t -> true;
    for (FilterCriterion c : criteria) {
      Predicate<T> p = resolver.apply(c);
      combined = combinator == FilterCombinator.OR ? combined.or(p) : combined.and(p);
    }
    return combined;
  }
}
