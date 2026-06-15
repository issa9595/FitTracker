package com.fittracker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittracker.notification.Notification;
import com.fittracker.notification.NotificationRepository;
import com.fittracker.notification.NotificationType;
import com.fittracker.support.AbstractIntegrationTest;
import com.fittracker.support.rgpd.UserAnonymizationService;
import com.fittracker.training.SessionType;
import com.fittracker.training.TrainingSession;
import com.fittracker.training.TrainingSessionRepository;
import com.fittracker.user.Profile;
import com.fittracker.user.ProfileRepository;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Pipeline RGPD : DELETE /me anonymise les sessions et purge les donnees personnelles. */
@Transactional
class RgpdPipelineIT extends AbstractIntegrationTest {

  @Autowired UserRepository users;
  @Autowired ProfileRepository profiles;
  @Autowired TrainingSessionRepository sessions;
  @Autowired NotificationRepository notifications;
  @Autowired UserAnonymizationService anonymization;

  @PersistenceContext EntityManager em;

  @Test
  void delete_me_anonymizes_sessions_and_purges_personal_data() {
    var user =
        users.save(
            new User(
                UUID.randomUUID(), "rgpd-" + UUID.randomUUID() + "@x.dev", "h", "Rgpd",
                OffsetDateTime.now()));
    var profile = new Profile(user.getId(), 180, 80.0, 78.0, "bio");
    profile.setUser(user);
    profiles.save(profile);

    var sessionId = UUID.randomUUID();
    sessions.save(
        new TrainingSession(
            sessionId, user.getId(), OffsetDateTime.now(), 1800, SessionType.RUNNING, null,
            OffsetDateTime.now()));

    notifications.save(
        new Notification(
            UUID.randomUUID(), user.getId(), NotificationType.NEW_PR, new HashMap<>(),
            OffsetDateTime.now()));

    anonymization.anonymize(user.getId());

    // Forcer la relecture depuis la base (le cache de 1er niveau masquerait le soft-delete).
    em.flush();
    em.clear();

    assertThat(users.findById(user.getId())).isEmpty(); // soft-delete masque par @SQLRestriction
    assertThat(profiles.findById(user.getId())).isEmpty();
    assertThat(notifications.findByUserId(user.getId())).isEmpty();
    // La session existe encore mais a ete reassignee au user sentinelle.
    assertThat(sessions.findByUserId(UserAnonymizationService.DELETED_SENTINEL_ID))
        .extracting(TrainingSession::getId)
        .contains(sessionId);
  }
}
