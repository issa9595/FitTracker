package com.fittracker.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.hateoas.RepresentationModel;

@Schema(description = "Relation de suivi entre deux utilisateurs")
public class FollowResponse extends RepresentationModel<FollowResponse> {
  private UUID followerId;
  private UUID followeeId;
  private OffsetDateTime createdAt;

  public FollowResponse() {}

  public FollowResponse(UUID followerId, UUID followeeId, OffsetDateTime createdAt) {
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
}
