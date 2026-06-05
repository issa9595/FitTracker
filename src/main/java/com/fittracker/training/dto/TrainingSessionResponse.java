package com.fittracker.training.dto;

import com.fittracker.training.SessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.hateoas.RepresentationModel;

@Schema(description = "Session d'entrainement")
public class TrainingSessionResponse extends RepresentationModel<TrainingSessionResponse> {
  private UUID id;
  private UUID userId;
  private OffsetDateTime startedAt;
  private int durationSeconds;
  private SessionType type;
  private String notes;
  private OffsetDateTime createdAt;
  private List<SessionExerciseResponse> exercises;

  public TrainingSessionResponse() {}

  public TrainingSessionResponse(
      UUID id,
      UUID userId,
      OffsetDateTime startedAt,
      int durationSeconds,
      SessionType type,
      String notes,
      OffsetDateTime createdAt,
      List<SessionExerciseResponse> exercises) {
    this.id = id;
    this.userId = userId;
    this.startedAt = startedAt;
    this.durationSeconds = durationSeconds;
    this.type = type;
    this.notes = notes;
    this.createdAt = createdAt;
    this.exercises = exercises;
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

  public List<SessionExerciseResponse> getExercises() {
    return exercises;
  }
}
