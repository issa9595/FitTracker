package com.fittracker.training;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repo des sessions. Etend {@link JpaSpecificationExecutor} pour permettre le filtrage dynamique
 * multi-champs (Specifications) exige par le livrable 3.
 */
public interface TrainingSessionRepository
    extends JpaRepository<TrainingSession, UUID>, JpaSpecificationExecutor<TrainingSession> {

  List<TrainingSession> findByUserId(UUID userId);

  Page<TrainingSession> findByUserId(UUID userId, Pageable pageable);

  long countByUserId(UUID userId);
}
