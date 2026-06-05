package com.fittracker.training;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Seed du referentiel d'exercices. Sera migre en Flyway V2__seed_exercises.sql en Phase 4. */
@Component
public class ExerciseSeed {

  private final ExerciseRepository repository;

  public ExerciseSeed(ExerciseRepository repository) {
    this.repository = repository;
  }

  @PostConstruct
  public void seed() {
    if (repository.count() > 0) {
      return;
    }
    List<Exercise> exercises =
        List.of(
            new Exercise(uuid("a1"), "Course a pied", ExerciseCategory.RUNNING, "cardio", ExerciseUnit.DISTANCE),
            new Exercise(uuid("a2"), "Course fractionnee", ExerciseCategory.RUNNING, "cardio", ExerciseUnit.TIME),
            new Exercise(uuid("b1"), "Developpe couche", ExerciseCategory.STRENGTH, "pectoraux", ExerciseUnit.REPS),
            new Exercise(uuid("b2"), "Squat", ExerciseCategory.STRENGTH, "jambes", ExerciseUnit.REPS),
            new Exercise(uuid("b3"), "Souleve de terre", ExerciseCategory.STRENGTH, "dos", ExerciseUnit.REPS),
            new Exercise(uuid("b4"), "Tractions", ExerciseCategory.STRENGTH, "dos", ExerciseUnit.REPS),
            new Exercise(uuid("c1"), "Jab cross", ExerciseCategory.MMA, "full body", ExerciseUnit.REPS),
            new Exercise(uuid("c2"), "Low kick", ExerciseCategory.MMA, "jambes", ExerciseUnit.REPS),
            new Exercise(uuid("c3"), "Sac de frappe", ExerciseCategory.MMA, "full body", ExerciseUnit.TIME),
            new Exercise(uuid("d1"), "Single leg takedown", ExerciseCategory.WRESTLING, "full body", ExerciseUnit.REPS),
            new Exercise(uuid("d2"), "Sprawl", ExerciseCategory.WRESTLING, "full body", ExerciseUnit.REPS),
            new Exercise(uuid("e1"), "Etirement", ExerciseCategory.OTHER, "mobilite", ExerciseUnit.TIME));
    exercises.forEach(repository::save);
  }

  private static UUID uuid(String suffix) {
    return UUID.fromString("00000000-0000-0000-0000-0000000000" + suffix);
  }
}
