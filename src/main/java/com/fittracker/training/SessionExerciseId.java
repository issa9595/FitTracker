package com.fittracker.training;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Cle composite de {@link SessionExercise} : (session_id, exercise_id, position). */
@Embeddable
public class SessionExerciseId implements Serializable {

  @Column(name = "session_id")
  private UUID sessionId;

  @Column(name = "exercise_id")
  private UUID exerciseId;

  @Column(name = "position")
  private int position;

  public SessionExerciseId() {}

  public SessionExerciseId(UUID sessionId, UUID exerciseId, int position) {
    this.sessionId = sessionId;
    this.exerciseId = exerciseId;
    this.position = position;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SessionExerciseId other)) {
      return false;
    }
    return position == other.position
        && Objects.equals(sessionId, other.sessionId)
        && Objects.equals(exerciseId, other.exerciseId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sessionId, exerciseId, position);
  }
}
