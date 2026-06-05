package com.fittracker.user;

import com.fittracker.common.repository.InMemoryRepository;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository extends InMemoryRepository<Profile, UUID> {
  @Override
  protected UUID idOf(Profile entity) {
    return entity.getUserId();
  }
}
