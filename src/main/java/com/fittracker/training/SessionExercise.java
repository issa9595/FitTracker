package com.fittracker.training;

import java.util.UUID;

public final class SessionExercise {
  private UUID sessionId;
  private UUID exerciseId;
  private int position;
  private Integer sets;
  private Integer reps;
  private Double weightKg;
  private Integer distanceM;
  private Integer timeSeconds;

  public SessionExercise() {}

  public SessionExercise(
      UUID sessionId,
      UUID exerciseId,
      int position,
      Integer sets,
      Integer reps,
      Double weightKg,
      Integer distanceM,
      Integer timeSeconds) {
    this.sessionId = sessionId;
    this.exerciseId = exerciseId;
    this.position = position;
    this.sets = sets;
    this.reps = reps;
    this.weightKg = weightKg;
    this.distanceM = distanceM;
    this.timeSeconds = timeSeconds;
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public UUID getExerciseId() {
    return exerciseId;
  }

  public int getPosition() {
    return position;
  }

  public Integer getSets() {
    return sets;
  }

  public Integer getReps() {
    return reps;
  }

  public Double getWeightKg() {
    return weightKg;
  }

  public Integer getDistanceM() {
    return distanceM;
  }

  public Integer getTimeSeconds() {
    return timeSeconds;
  }
}
