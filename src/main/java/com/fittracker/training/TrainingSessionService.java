package com.fittracker.training;

import com.fittracker.common.error.ForbiddenException;
import com.fittracker.common.error.NotFoundException;
import com.fittracker.notification.event.NewPrEvent;
import com.fittracker.notification.event.SessionCompletedEvent;
import com.fittracker.training.dto.SessionExerciseRequest;
import com.fittracker.training.dto.TrainingSessionCreateRequest;
import com.fittracker.training.dto.TrainingSessionUpdateRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingSessionService {

  private final TrainingSessionRepository sessionRepository;
  private final ExerciseRepository exerciseRepository;
  private final ApplicationEventPublisher eventPublisher;

  public TrainingSessionService(
      TrainingSessionRepository sessionRepository,
      ExerciseRepository exerciseRepository,
      ApplicationEventPublisher eventPublisher) {
    this.sessionRepository = sessionRepository;
    this.exerciseRepository = exerciseRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
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
    TrainingSession saved = sessionRepository.save(session);
    // Notifie les followers en temps reel (apres commit, cf. NotificationListener).
    eventPublisher.publishEvent(new SessionCompletedEvent(userId, saved.getId()));
    return saved;
  }

  @Transactional(readOnly = true)
  public TrainingSession getOwned(UUID sessionId, UUID userId) {
    TrainingSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new NotFoundException("TrainingSession", sessionId));
    if (!session.getUserId().equals(userId)) {
      throw new ForbiddenException("Cette session ne vous appartient pas");
    }
    // La reponse DTO inclut les exercices : on initialise la collection LAZY dans la transaction
    // (OSIV desactive) pour que le mapper s'execute sans LazyInitializationException.
    Hibernate.initialize(session.getExercises());
    return session;
  }

  @Transactional
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

  @Transactional
  public void delete(UUID sessionId, UUID userId) {
    getOwned(sessionId, userId);
    sessionRepository.deleteById(sessionId);
  }

  @Transactional(readOnly = true)
  public List<TrainingSession> listForUser(UUID userId) {
    List<TrainingSession> sessions = sessionRepository.findByUserId(userId);
    sessions.forEach(session -> Hibernate.initialize(session.getExercises()));
    return sessions;
  }

  @Transactional
  public TrainingSession addExercise(UUID sessionId, UUID userId, SessionExerciseRequest req) {
    TrainingSession session = getOwned(sessionId, userId);
    Exercise exercise =
        exerciseRepository
            .findById(req.exerciseId())
            .orElseThrow(() -> new NotFoundException("Exercise", req.exerciseId()));
    Double previousMaxWeight =
        sessionRepository.findMaxWeightForUserAndExercise(userId, exercise.getId());
    int position = req.position() != null ? req.position() : session.getExercises().size();
    SessionExercise se =
        new SessionExercise(
            sessionId,
            exercise.getId(),
            position,
            req.sets(),
            req.reps(),
            req.weightKg(),
            req.distanceM(),
            req.timeSeconds());
    // @MapsId : renseigner les deux cotes avant la cascade depuis la session.
    se.setSession(session);
    se.setExercise(exercise);
    session.getExercises().add(se);
    TrainingSession saved = sessionRepository.save(session);

    // Detection de PR : la charge ajoutee bat le precedent maximum de l'utilisateur sur l'exercice.
    Double weight = req.weightKg();
    if (weight != null && (previousMaxWeight == null || weight > previousMaxWeight)) {
      eventPublisher.publishEvent(new NewPrEvent(userId, exercise.getId(), weight));
    }
    return saved;
  }
}
