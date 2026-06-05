package com.fittracker.social;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Follow {
  private UUID followerId;
  private UUID followeeId;
  private OffsetDateTime createdAt;

  public Follow() {}

  public Follow(UUID followerId, UUID followeeId, OffsetDateTime createdAt) {
    this.followerId = followerId;
    this.followeeId = followeeId;
    this.createdAt = createdAt;
  }

  public UUID getFollowerId() {
    return followerId;
  }

  public UUID getFolloweeId() {
    return followeeId;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public FollowKey key() {
    return new FollowKey(followerId, followeeId);
  }

  public record FollowKey(UUID followerId, UUID followeeId) {
    public FollowKey {
      Objects.requireNonNull(followerId);
      Objects.requireNonNull(followeeId);
    }
  }
}
