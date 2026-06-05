package com.fittracker.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.hateoas.RepresentationModel;

@Schema(description = "Utilisateur expose")
public class UserResponse extends RepresentationModel<UserResponse> {
  private UUID id;
  private String email;
  private String displayName;
  private OffsetDateTime createdAt;

  public UserResponse() {}

  public UserResponse(UUID id, String email, String displayName, OffsetDateTime createdAt) {
    this.id = id;
    this.email = email;
    this.displayName = displayName;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
