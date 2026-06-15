package com.fittracker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittracker.support.AbstractIntegrationTest;
import com.fittracker.training.SessionType;
import com.fittracker.training.TrainingSession;
import com.fittracker.training.TrainingSessionRepository;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Relation One-to-Many : un User possede plusieurs TrainingSession. */
@Transactional
class OneToManyRelationIT extends AbstractIntegrationTest {

  @Autowired UserRepository users;
  @Autowired TrainingSessionRepository sessions;

  @Test
  void user_owns_many_sessions() {
    var user =
        users.save(
            new User(
                UUID.randomUUID(),
                "bob-" + UUID.randomUUID() + "@fittracker.dev",
                "h",
                "Bob",
                OffsetDateTime.now()));

    sessions.save(
        new TrainingSession(
            UUID.randomUUID(), user.getId(), OffsetDateTime.now(), 1800, SessionType.RUNNING, null,
            OffsetDateTime.now()));
    sessions.save(
        new TrainingSession(
            UUID.randomUUID(), user.getId(), OffsetDateTime.now(), 3600, SessionType.STRENGTH, null,
            OffsetDateTime.now()));

    assertThat(sessions.findByUserId(user.getId())).hasSize(2);
    assertThat(sessions.countByUserId(user.getId())).isEqualTo(2);
  }
}
