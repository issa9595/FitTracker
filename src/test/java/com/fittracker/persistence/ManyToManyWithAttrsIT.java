package com.fittracker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittracker.support.AbstractIntegrationTest;
import com.fittracker.training.Exercise;
import com.fittracker.training.ExerciseCategory;
import com.fittracker.training.ExerciseRepository;
import com.fittracker.training.ExerciseUnit;
import com.fittracker.training.SessionExercise;
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

/** Relation Many-to-Many avec attributs : TrainingSession <-> Exercise via SessionExercise. */
@Transactional
class ManyToManyWithAttrsIT extends AbstractIntegrationTest {

  @Autowired UserRepository users;
  @Autowired TrainingSessionRepository sessions;
  @Autowired ExerciseRepository exercises;

  @Test
  void session_links_exercises_with_attributes() {
    var user =
        users.save(
            new User(
                UUID.randomUUID(), "carol-" + UUID.randomUUID() + "@x.dev", "h", "Carol",
                OffsetDateTime.now()));
    var session =
        sessions.save(
            new TrainingSession(
                UUID.randomUUID(), user.getId(), OffsetDateTime.now(), 1200, SessionType.STRENGTH,
                null, OffsetDateTime.now()));
    var squat =
        exercises.save(
            new Exercise(
                UUID.randomUUID(), "Squat test", ExerciseCategory.STRENGTH, "jambes",
                ExerciseUnit.REPS));

    var se = new SessionExercise(session.getId(), squat.getId(), 0, 4, 8, 80.0, null, null);
    se.setSession(session);
    se.setExercise(squat);
    session.getExercises().add(se);
    sessions.save(session);

    var reloaded = sessions.findById(session.getId()).orElseThrow();
    assertThat(reloaded.getExercises()).hasSize(1);
    assertThat(reloaded.getExercises().get(0).getReps()).isEqualTo(8);
    assertThat(reloaded.getExercises().get(0).getWeightKg()).isEqualTo(80.0);
    assertThat(reloaded.getExercises().get(0).getExerciseId()).isEqualTo(squat.getId());
  }
}
