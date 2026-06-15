package com.fittracker.social;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

  List<Follow> findByIdFollowerId(UUID followerId);

  List<Follow> findByIdFolloweeId(UUID followeeId);

  @Transactional
  long deleteByIdFollowerIdAndIdFolloweeId(UUID followerId, UUID followeeId);

  @Transactional
  long deleteByIdFollowerIdOrIdFolloweeId(UUID followerId, UUID followeeId);
}
