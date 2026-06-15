package com.fittracker.training;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Entite JPA Exercise : referentiel partage (seede via Flyway V2). */
@Entity
@Table(name = "exercises")
public class Exercise {

  @Id private UUID id;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ExerciseCategory category;

  @Column(name = "muscle_group", nullable = false, length = 60)
  private String muscleGroup;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ExerciseUnit unit;

  public Exercise() {}

  public Exercise(
      UUID id, String name, ExerciseCategory category, String muscleGroup, ExerciseUnit unit) {
    this.id = id;
    this.name = name;
    this.category = category;
    this.muscleGroup = muscleGroup;
    this.unit = unit;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public ExerciseCategory getCategory() {
    return category;
  }

  public String getMuscleGroup() {
    return muscleGroup;
  }

  public ExerciseUnit getUnit() {
    return unit;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setCategory(ExerciseCategory category) {
    this.category = category;
  }

  public void setMuscleGroup(String muscleGroup) {
    this.muscleGroup = muscleGroup;
  }

  public void setUnit(ExerciseUnit unit) {
    this.unit = unit;
  }
}
