package com.fittracker.training;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Entite d'association M-N entre {@link TrainingSession} et {@link Exercise} portant des attributs
 * (sets, reps, charge, distance, temps). Cle composite via {@link SessionExerciseId}.
 */
@Entity
@Table(name = "session_exercises")
public class SessionExercise {

  @EmbeddedId private SessionExerciseId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("sessionId")
  @JoinColumn(name = "session_id")
  private TrainingSession session;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("exerciseId")
  @JoinColumn(name = "exercise_id")
  private Exercise exercise;

  private Integer sets;
  private Integer reps;

  @Column(name = "weight_kg")
  private Double weightKg;

  @Column(name = "distance_m")
  private Integer distanceM;

  @Column(name = "time_seconds")
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
    this.id = new SessionExerciseId(sessionId, exerciseId, position);
    this.sets = sets;
    this.reps = reps;
    this.weightKg = weightKg;
    this.distanceM = distanceM;
    this.timeSeconds = timeSeconds;
  }

  public SessionExerciseId getId() {
    return id;
  }

  public UUID getSessionId() {
    return id != null ? id.getSessionId() : null;
  }

  public UUID getExerciseId() {
    return id != null ? id.getExerciseId() : null;
  }

  public int getPosition() {
    return id != null ? id.getPosition() : 0;
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

  public TrainingSession getSession() {
    return session;
  }

  public void setSession(TrainingSession session) {
    this.session = session;
  }

  public Exercise getExercise() {
    return exercise;
  }

  public void setExercise(Exercise exercise) {
    this.exercise = exercise;
  }
}
