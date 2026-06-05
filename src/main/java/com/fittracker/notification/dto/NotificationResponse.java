package com.fittracker.notification.dto;

import com.fittracker.notification.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.hateoas.RepresentationModel;

@Schema(description = "Notification utilisateur")
public class NotificationResponse extends RepresentationModel<NotificationResponse> {
  private UUID id;
  private NotificationType type;
  private Map<String, Object> payload;
  private OffsetDateTime readAt;
  private OffsetDateTime createdAt;

  public NotificationResponse() {}

  public NotificationResponse(
      UUID id,
      NotificationType type,
      Map<String, Object> payload,
      OffsetDateTime readAt,
      OffsetDateTime createdAt) {
    this.id = id;
    this.type = type;
    this.payload = payload;
    this.readAt = readAt;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
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

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public boolean isRead() {
    return readAt != null;
  }
}
