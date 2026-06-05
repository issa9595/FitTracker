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
            ex("a1", "Course a pied", ExerciseCategory.RUNNING, "cardio", ExerciseUnit.DISTANCE),
            ex("a2", "Course fractionnee", ExerciseCategory.RUNNING, "cardio", ExerciseUnit.TIME),
            ex("b1", "Developpe couche", ExerciseCategory.STRENGTH, "pectoraux", ExerciseUnit.REPS),
            ex("b2", "Squat", ExerciseCategory.STRENGTH, "jambes", ExerciseUnit.REPS),
            ex("b3", "Souleve de terre", ExerciseCategory.STRENGTH, "dos", ExerciseUnit.REPS),
            ex("b4", "Tractions", ExerciseCategory.STRENGTH, "dos", ExerciseUnit.REPS),
            ex("c1", "Jab cross", ExerciseCategory.MMA, "full body", ExerciseUnit.REPS),
            ex("c2", "Low kick", ExerciseCategory.MMA, "jambes", ExerciseUnit.REPS),
            ex("c3", "Sac de frappe", ExerciseCategory.MMA, "full body", ExerciseUnit.TIME),
            ex("d1", "Single leg takedown", ExerciseCategory.WRESTLING, "fullbody", ExerciseUnit.REPS),
            ex("d2", "Sprawl", ExerciseCategory.WRESTLING, "full body", ExerciseUnit.REPS),
            ex("e1", "Etirement", ExerciseCategory.OTHER, "mobilite", ExerciseUnit.TIME));
    exercises.forEach(repository::save);
  }

  private static Exercise ex(
      String idSuffix, String name, ExerciseCategory category, String muscleGroup, ExerciseUnit unit) {
    return new Exercise(uuid(idSuffix), name, category, muscleGroup, unit);
  }

  private static UUID uuid(String suffix) {
    return UUID.fromString("00000000-0000-0000-0000-0000000000" + suffix);
  }
}
