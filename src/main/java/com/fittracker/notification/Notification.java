package com.fittracker.notification;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public final class Notification {
  private UUID id;
  private UUID userId;
  private NotificationType type;
  private Map<String, Object> payload;
  private OffsetDateTime readAt;
  private OffsetDateTime createdAt;

  public Notification() {}

  public Notification(
      UUID id,
      UUID userId,
      NotificationType type,
      Map<String, Object> payload,
      OffsetDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.type = type;
    this.payload = payload;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public NotificationType getType() {
    return type;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public OffsetDateTime getReadAt() {
    return readAt;
  }

  public void setReadAt(OffsetDateTime readAt) {
    this.readAt = readAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
