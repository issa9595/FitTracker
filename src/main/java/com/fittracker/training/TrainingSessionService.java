package com.fittracker.training;

import com.fittracker.common.error.ForbiddenException;
import com.fittracker.common.error.NotFoundException;
import com.fittracker.training.dto.SessionExerciseRequest;
import com.fittracker.training.dto.TrainingSessionCreateRequest;
import com.fittracker.training.dto.TrainingSessionUpdateRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TrainingSessionService {

  private final TrainingSessionRepository sessionRepository;
  private final ExerciseRepository exerciseRepository;

  public TrainingSessionService(
      TrainingSessionRepository sessionRepository, ExerciseRepository exerciseRepository) {
    this.sessionRepository = sessionRepository;
    this.exerciseRepository = exerciseRepository;
  }

  public TrainingSession create(UUID userId, TrainingSessionCreateRequest req) {
    TrainingSession session =
        new TrainingSession(
            UUID.randomUUID(),
            userId,
            req.startedAt(),
            req.durationSeconds(),
            req.type(),
            req.notes(),
            OffsetDateTime.now());
    return sessionRepository.save(session);
  }

  public TrainingSession getOwned(UUID sessionId, UUID userId) {
    TrainingSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new NotFoundException("TrainingSession", sessionId));
    if (!session.getUserId().equals(userId)) {
      throw new ForbiddenException("Cette session ne vous appartient pas");
    }
    return session;
  }

  public TrainingSession update(UUID sessionId, UUID userId, TrainingSessionUpdateRequest req) {
    TrainingSession session = getOwned(sessionId, userId);
    if (req.startedAt() != null) {
      session.setStartedAt(req.startedAt());
    }
    if (req.durationSeconds() != null) {
      session.setDurationSeconds(req.durationSeconds());
    }
    if (req.type() != null) {
      session.setType(req.type());
    }
    if (req.notes() != null) {
      session.setNotes(req.notes());
    }
    return sessionRepository.save(session);
  }

  public void delete(UUID sessionId, UUID userId) {
    getOwned(sessionId, userId);
    sessionRepository.deleteById(sessionId);
  }

  public List<TrainingSession> listForUser(UUID userId) {
    return sessionRepository.findByUserId(userId);
  }

  public TrainingSession addExercise(UUID sessionId, UUID userId, SessionExerciseRequest req) {
    TrainingSession session = getOwned(sessionId, userId);
    if (!exerciseRepository.existsById(req.exerciseId())) {
      throw new NotFoundException("Exercise", req.exerciseId());
    }
    int position = req.position() != null ? req.position() : session.getExercises().size();
    session
        .getExercises()
        .add(
            new SessionExercise(
                sessionId,
                req.exerciseId(),
                position,
                req.sets(),
                req.reps(),
                req.weightKg(),
                req.distanceM(),
                req.timeSeconds()));
    return sessionRepository.save(session);
  }
}
