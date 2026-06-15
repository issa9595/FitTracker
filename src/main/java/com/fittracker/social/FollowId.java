package com.fittracker.social;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Cle composite de {@link Follow} : (follower_id, followee_id). */
@Embeddable
public class FollowId implements Serializable {

  @Column(name = "follower_id")
  private UUID followerId;

  @Column(name = "followee_id")
  private UUID followeeId;

  public FollowId() {}

  public FollowId(UUID followerId, UUID followeeId) {
    this.followerId = Objects.requireNonNull(followerId);
    this.followeeId = Objects.requireNonNull(followeeId);
  }

  public UUID getFollowerId() {
    return followerId;
  }

  public UUID getFolloweeId() {
    return followeeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FollowId other)) {
      return false;
    }
    return Objects.equals(followerId, other.followerId)
        && Objects.equals(followeeId, other.followeeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(followerId, followeeId);
  }
}
