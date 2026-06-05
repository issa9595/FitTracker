package com.fittracker.training;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TrainingSession {
  private UUID id;
  private UUID userId;
  private OffsetDateTime startedAt;
  private int durationSeconds;
  private SessionType type;
  private String notes;
  private OffsetDateTime createdAt;
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
