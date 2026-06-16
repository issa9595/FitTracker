package com.fittracker.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fittracker.common.error.ForbiddenException;
import com.fittracker.common.error.NotFoundException;
import com.fittracker.notification.event.NewPrEvent;
import com.fittracker.notification.event.SessionCompletedEvent;
import com.fittracker.training.dto.SessionExerciseRequest;
import com.fittracker.training.dto.TrainingSessionCreateRequest;
import com.fittracker.training.dto.TrainingSessionUpdateRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** Tests unitaires (Mockito + AssertJ) du {@link TrainingSessionService}. */
@ExtendWith(MockitoExtension.class)
class TrainingSessionServiceTest {

  @Mock private TrainingSessionRepository sessionRepository;
  @Mock private ExerciseRepository exerciseRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private TrainingSessionService service;

  private final UUID userId = UUID.randomUUID();
  private final UUID sessionId = UUID.randomUUID();
  private final UUID exerciseId = UUID.randomUUID();
  private final OffsetDateTime startedAt = OffsetDateTime.parse("2026-06-04T10:00:00Z");

  @BeforeEach
  void setUp() {
    service = new TrainingSessionService(sessionRepository, exerciseRepository, eventPublisher);
  }

  private TrainingSession ownedSession() {
    return new TrainingSession(
        sessionId, userId, startedAt, 3600, SessionType.STRENGTH, "notes", null);
  }

  private Exercise exercise() {
    return new Exercise(exerciseId, "Bench", ExerciseCategory.STRENGTH, "chest", ExerciseUnit.REPS);
  }

  @Test
  void should_create_session_and_publish_event() {
    when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

    TrainingSession created =
        service.create(
            userId,
            new TrainingSessionCreateRequest(startedAt, 3600, SessionType.STRENGTH, "Push"));

    assertThat(created.getUserId()).isEqualTo(userId);
    assertThat(created.getType()).isEqualTo(SessionType.STRENGTH);
    verify(eventPublisher).publishEvent(any(SessionCompletedEvent.class));
  }

  @Test
  void should_return_session_when_owned() {
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(ownedSession()));

    assertThat(service.getOwned(sessionId, userId).getId()).isEqualTo(sessionId);
  }

  @Test
  void should_throw_not_found_when_session_absent() {
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getOwned(sessionId, userId))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void should_throw_forbidden_when_session_not_owned() {
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(ownedSession()));

    assertThatThrownBy(() -> service.getOwned(sessionId, UUID.randomUUID()))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void should_update_all_fields_when_provided() {
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(ownedSession()));
    when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

    OffsetDateTime newStart = OffsetDateTime.parse("2026-06-05T08:00:00Z");
    TrainingSession updated =
        service.update(
            sessionId,
            userId,
            new TrainingSessionUpdateRequest(newStart, 7200, SessionType.RUNNING, "Leg day"));

    assertThat(updated.getStartedAt()).isEqualTo(newStart);
    assertThat(updated.getDurationSeconds()).isEqualTo(7200);
    assertThat(updated.getType()).isEqualTo(SessionType.RUNNING);
    assertThat(updated.getNotes()).isEqualTo("Leg day");
  }

  @Test
  void should_keep_fields_when_update_is_null() {
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(ownedSession()));
    when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

    TrainingSession updated =
        service.update(
            sessionId, userId, new TrainingSessionUpdateRequest(null, null, null, null));

    assertThat(updated.getType()).isEqualTo(SessionType.STRENGTH);
    assertThat(updated.getDurationSeconds()).isEqualTo(3600);
  }

  @Test
  void should_delete_owned_session() {
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(ownedSession()));

    service.delete(sessionId, userId);

    verify(sessionRepository).deleteById(sessionId);
  }

  @Test
  void should_list_sessions_for_user() {
    List<TrainingSession> sessions = List.of(ownedSession());
    when(sessionRepository.findByUserId(userId)).thenReturn(sessions);

    assertThat(service.listForUser(userId)).isEqualTo(sessions);
  }

  @Test
  void should_add_exercise_and_publish_pr_when_weight_is_new_record() {
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(ownedSession()));
    when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise()));
    when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

    TrainingSession saved =
        service.addExercise(
            sessionId,
            userId,
            new SessionExerciseRequest(exerciseId, 0, 4, 10, 100.0, null, null));

    assertThat(saved.getExercises()).hasSize(1);
    verify(eventPublisher).publishEvent(any(NewPrEvent.class));
  }

  @Test
  void should_add_exercise_without_pr_when_weight_absent() {
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(ownedSession()));
    when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise()));
    when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

    service.addExercise(
        sessionId, userId, new SessionExerciseRequest(exerciseId, null, 3, 12, null, null, null));

    verify(eventPublisher, never()).publishEvent(any(NewPrEvent.class));
  }

  @Test
  void should_not_publish_pr_when_weight_below_previous_max() {
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(ownedSession()));
    when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise()));
    when(sessionRepository.findMaxWeightForUserAndExercise(userId, exerciseId)).thenReturn(200.0);
    when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

    service.addExercise(
        sessionId, userId, new SessionExerciseRequest(exerciseId, 0, 4, 10, 100.0, null, null));

    verify(eventPublisher, never()).publishEvent(any(NewPrEvent.class));
  }

  @Test
  void should_throw_not_found_when_exercise_absent() {
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(ownedSession()));
    when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.addExercise(
                    sessionId,
                    userId,
                    new SessionExerciseRequest(exerciseId, 0, 4, 10, 100.0, null, null)))
        .isInstanceOf(NotFoundException.class);
  }
}
