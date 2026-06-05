package com.fittracker.training;

import com.fittracker.common.repository.InMemoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class TrainingSessionRepository extends InMemoryRepository<TrainingSession, UUID> {

  @Override
  protected UUID idOf(TrainingSession entity) {
    return entity.getId();
  }

  public List<TrainingSession> findByUserId(UUID userId) {
    return stream().filter(s -> userId.equals(s.getUserId())).toList();
  }
}
