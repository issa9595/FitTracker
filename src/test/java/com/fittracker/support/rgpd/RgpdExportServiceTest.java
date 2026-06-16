package com.fittracker.support.rgpd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fittracker.common.error.NotFoundException;
import com.fittracker.notification.Notification;
import com.fittracker.notification.NotificationRepository;
import com.fittracker.notification.NotificationType;
import com.fittracker.social.Follow;
import com.fittracker.social.FollowRepository;
import com.fittracker.training.Program;
import com.fittracker.training.ProgramRepository;
import com.fittracker.training.SessionExercise;
import com.fittracker.training.SessionType;
import com.fittracker.training.TrainingSession;
import com.fittracker.training.TrainingSessionRepository;
import com.fittracker.user.Profile;
import com.fittracker.user.ProfileRepository;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests unitaires (Mockito + AssertJ) du {@link RgpdExportService} (droit a la portabilite). */
@ExtendWith(MockitoExtension.class)
class RgpdExportServiceTest {

  @Mock private UserRepository users;
  @Mock private ProfileRepository profiles;
  @Mock private TrainingSessionRepository sessions;
  @Mock private ProgramRepository programs;
  @Mock private FollowRepository follows;
  @Mock private NotificationRepository notifications;

  private RgpdExportService service;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new RgpdExportService(users, profiles, sessions, programs, follows, notifications);
  }

  @Test
  void should_throw_not_found_when_user_absent() {
    when(users.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.exportFor(userId)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void should_aggregate_all_user_data_when_present() {
    UUID sessionId = UUID.randomUUID();
    User user = new User(userId, "jane@fit.io", "hash", "Jane", OffsetDateTime.now());
    Profile profile = new Profile(userId, 180, 75.0, 72.0, "bio");
    TrainingSession session =
        new TrainingSession(
            sessionId, userId, OffsetDateTime.now(), 3600, SessionType.STRENGTH, "notes", null);
    session
        .getExercises()
        .add(new SessionExercise(sessionId, UUID.randomUUID(), 0, 4, 10, 100.0, 5000, 1800));
    Program program =
        new Program(
            UUID.randomUUID(),
            userId,
            "Prep",
            "desc",
            "10km",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 9, 1),
            OffsetDateTime.now());
    Follow follow = new Follow(UUID.randomUUID(), userId, OffsetDateTime.now());
    Notification notification =
        new Notification(
            UUID.randomUUID(),
            userId,
            NotificationType.NEW_PR,
            Map.of("weightKg", 100.0),
            OffsetDateTime.now());

    when(users.findById(userId)).thenReturn(Optional.of(user));
    when(profiles.findById(userId)).thenReturn(Optional.of(profile));
    when(sessions.findByUserId(userId)).thenReturn(List.of(session));
    when(programs.findByUserId(userId)).thenReturn(List.of(program));
    when(follows.findByIdFolloweeId(userId)).thenReturn(List.of(follow));
    when(follows.findByIdFollowerId(userId)).thenReturn(List.of(follow));
    when(notifications.findByUserId(userId)).thenReturn(List.of(notification));

    Map<String, Object> export = service.exportFor(userId);

    assertThat(export)
        .containsKeys(
            "user", "profile", "trainingSessions", "programs", "followers", "following",
            "notifications");
    assertThat(export.get("profile")).isNotNull();
    assertThat((List<?>) export.get("trainingSessions")).hasSize(1);
    assertThat((List<?>) export.get("notifications")).hasSize(1);
  }

  @Test
  void should_return_null_profile_when_profile_absent() {
    User user = new User(userId, "jane@fit.io", "hash", "Jane", OffsetDateTime.now());
    when(users.findById(userId)).thenReturn(Optional.of(user));
    when(profiles.findById(userId)).thenReturn(Optional.empty());
    when(sessions.findByUserId(userId)).thenReturn(List.of());
    when(programs.findByUserId(userId)).thenReturn(List.of());
    when(follows.findByIdFolloweeId(userId)).thenReturn(List.of());
    when(follows.findByIdFollowerId(userId)).thenReturn(List.of());
    when(notifications.findByUserId(userId)).thenReturn(List.of());

    Map<String, Object> export = service.exportFor(userId);

    assertThat(export.get("profile")).isNull();
    assertThat((List<?>) export.get("programs")).isEmpty();
  }
}
