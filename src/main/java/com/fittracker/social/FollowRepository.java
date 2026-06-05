package com.fittracker.social;

import com.fittracker.common.repository.InMemoryRepository;
import com.fittracker.social.Follow.FollowKey;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class FollowRepository extends InMemoryRepository<Follow, FollowKey> {
  @Override
  protected FollowKey idOf(Follow entity) {
    return entity.key();
  }

  public List<Follow> findByFollower(UUID followerId) {
    return stream().filter(f -> followerId.equals(f.getFollowerId())).toList();
  }

  public List<Follow> findByFollowee(UUID followeeId) {
    return stream().filter(f -> followeeId.equals(f.getFolloweeId())).toList();
  }
}
