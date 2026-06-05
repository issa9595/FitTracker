package com.fittracker.training;

import com.fittracker.common.repository.InMemoryRepository;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ExerciseRepository extends InMemoryRepository<Exercise, UUID> {
  @Override
  protected UUID idOf(Exercise entity) {
    return entity.getId();
  }
}
