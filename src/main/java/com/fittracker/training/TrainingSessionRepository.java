package com.fittracker.training;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repo des sessions. Etend {@link JpaSpecificationExecutor} pour permettre le filtrage dynamique
 * multi-champs (Specifications) exige par le livrable 3.
 */
public interface TrainingSessionRepository
    extends JpaRepository<TrainingSession, UUID>, JpaSpecificationExecutor<TrainingSession> {

  List<TrainingSession> findByUserId(UUID userId);

  Page<TrainingSession> findByUserId(UUID userId, Pageable pageable);

  long countByUserId(UUID userId);

  /** Charge maximale deja enregistree par l'utilisateur sur un exercice (null si aucune). */
  @Query(
      "select max(se.weightKg) from TrainingSession s join s.exercises se "
          + "where s.userId = :userId and se.id.exerciseId = :exerciseId")
  Double findMaxWeightForUserAndExercise(
      @Param("userId") UUID userId, @Param("exerciseId") UUID exerciseId);
}
