package com.fittracker.training;

import com.fittracker.common.repository.InMemoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ProgramRepository extends InMemoryRepository<Program, UUID> {
  @Override
  protected UUID idOf(Program entity) {
    return entity.getId();
  }

  public List<Program> findByUserId(UUID userId) {
    return stream().filter(p -> userId.equals(p.getUserId())).toList();
  }
}
