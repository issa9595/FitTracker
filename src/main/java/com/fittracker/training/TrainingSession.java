package com.fittracker.training;

import com.fittracker.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Entite JPA TrainingSession (N-1 User, 1-N SessionExercise). */
@Entity
@Table(name = "training_sessions")
@EntityListeners(AuditingEntityListener.class)
public class TrainingSession {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;

  @Column(name = "started_at", nullable = false)
  private OffsetDateTime startedAt;

  @Column(name = "duration_seconds", nullable = false)
  private int durationSeconds;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SessionType type;

  @Column(length = 2000)
  private String notes;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Version private long version;

  @OneToMany(
      mappedBy = "session",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("id.position ASC")
  private final List<SessionExercise> exercises = new ArrayList<>();

  public TrainingSession() {}

  public TrainingSession(
      UUID id,
      UUID userId,
      OffsetDateTime startedAt,
      int durationSeconds,
      SessionType type,
      String notes,
      OffsetDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.startedAt = startedAt;
    this.durationSeconds = durationSeconds;
    this.type = type;
    this.notes = notes;
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

  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public int getDurationSeconds() {
    return durationSeconds;
  }

  public SessionType getType() {
    return type;
  }

  public String getNotes() {
    return notes;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }

  public List<SessionExercise> getExercises() {
    return exercises;
  }

  public void setStartedAt(OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public void setDurationSeconds(int durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  public void setType(SessionType type) {
    this.type = type;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
