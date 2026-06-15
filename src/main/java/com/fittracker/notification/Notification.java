package com.fittracker.notification;

import com.fittracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Entite JPA Notification (N-1 User) avec payload stocke en JSONB natif Postgres. */
@Entity
@Table(name = "notifications")
public class Notification {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private NotificationType type;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload = new HashMap<>();

  @Column(name = "read_at")
  private OffsetDateTime readAt;

  @Column(name = "created_at", nullable = false)
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
    this.payload = payload != null ? new HashMap<>(payload) : new HashMap<>();
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public User getUser() {
    return user;
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
