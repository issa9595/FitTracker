package com.fittracker.common.repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Base de depot in-memory pour la Phase 3. Sera remplacee par des JpaRepository en Phase 4
 * sans impact sur les services / controllers grace aux signatures stables (Optional, List).
 */
public abstract class InMemoryRepository<T, I> {

  protected final Map<I, T> store = new ConcurrentHashMap<>();

  protected abstract I idOf(T entity);

  public T save(T entity) {
    store.put(idOf(entity), entity);
    return entity;
  }

  public Optional<T> findById(I id) {
    return Optional.ofNullable(store.get(id));
  }

  public boolean existsById(I id) {
    return store.containsKey(id);
  }

  public boolean deleteById(I id) {
    return store.remove(id) != null;
  }

  public List<T> findAll() {
    return List.copyOf(store.values());
  }

  public long count() {
    return store.size();
  }

  public Collection<T> all() {
    return store.values();
  }

  protected Stream<T> stream() {
    return store.values().stream();
  }

  protected List<T> applyPredicateSortAndPage(
      Predicate<T> predicate, Comparator<T> comparator, int offset, int size) {
    Stream<T> filtered = stream().filter(predicate);
    if (comparator != null) {
      filtered = filtered.sorted(comparator);
    }
    return filtered.skip(offset).limit(size).toList();
  }

  protected long countMatching(Predicate<T> predicate) {
    return stream().filter(predicate).count();
  }
}
