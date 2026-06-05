package com.fittracker.training.mapper;

import com.fittracker.training.SessionExercise;
import com.fittracker.training.TrainingSession;
import com.fittracker.training.dto.SessionExerciseResponse;
import com.fittracker.training.dto.TrainingSessionResponse;
import org.springframework.stereotype.Component;

@Component
public class TrainingSessionMapper {

  public TrainingSessionResponse toResponse(TrainingSession session) {
    return new TrainingSessionResponse(
        session.getId(),
        session.getUserId(),
        session.getStartedAt(),
        session.getDurationSeconds(),
        session.getType(),
        session.getNotes(),
        session.getCreatedAt(),
        session.getExercises().stream().map(this::toExerciseResponse).toList());
  }

  public SessionExerciseResponse toExerciseResponse(SessionExercise e) {
    return new SessionExerciseResponse(
        e.getExerciseId(),
        e.getPosition(),
        e.getSets(),
        e.getReps(),
        e.getWeightKg(),
        e.getDistanceM(),
        e.getTimeSeconds());
  }
}
