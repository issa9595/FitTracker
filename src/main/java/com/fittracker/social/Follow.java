package com.fittracker.social;

import com.fittracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entite JPA Follow : relation M-N self-referencante User &lt;-&gt; User via une cle composite
 * {@link FollowId}.
 */
@Entity
@Table(name = "follows")
public class Follow {

  @EmbeddedId private FollowId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("followerId")
  @JoinColumn(name = "follower_id")
  private User follower;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("followeeId")
  @JoinColumn(name = "followee_id")
  private User followee;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public Follow() {}

  public Follow(UUID followerId, UUID followeeId, OffsetDateTime createdAt) {
    this.id = new FollowId(followerId, followeeId);
    this.createdAt = createdAt;
  }

  public FollowId getId() {
    return id;
  }

  public UUID getFollowerId() {
    return id != null ? id.getFollowerId() : null;
  }

  public UUID getFolloweeId() {
    return id != null ? id.getFolloweeId() : null;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public User getFollower() {
    return follower;
  }

  public void setFollower(User follower) {
    this.follower = follower;
  }

  public User getFollowee() {
    return followee;
  }

  public void setFollowee(User followee) {
    this.followee = followee;
  }

  public FollowKey key() {
    return new FollowKey(getFollowerId(), getFolloweeId());
  }

  /** Conserve pour retrocompatibilite de l'API interne (Phase 3). */
  public record FollowKey(UUID followerId, UUID followeeId) {
    public FollowKey {
      Objects.requireNonNull(followerId);
      Objects.requireNonNull(followeeId);
    }
  }
}
