package com.fittracker.user;

import com.fittracker.common.repository.InMemoryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends InMemoryRepository<User, UUID> {

  @Override
  protected UUID idOf(User entity) {
    return entity.getId();
  }

  public Optional<User> findByEmail(String email) {
    if (email == null) {
      return Optional.empty();
    }
    return stream().filter(u -> email.equalsIgnoreCase(u.getEmail())).findFirst();
  }

  public boolean existsByEmail(String email) {
    return findByEmail(email).isPresent();
  }
}
