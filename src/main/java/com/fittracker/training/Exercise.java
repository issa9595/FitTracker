package com.fittracker.training;

import java.util.UUID;

public final class Exercise {
  private UUID id;
  private String name;
  private ExerciseCategory category;
  private String muscleGroup;
  private ExerciseUnit unit;

  public Exercise() {}

  public Exercise(UUID id, String name, ExerciseCategory category, String muscleGroup, ExerciseUnit unit) {
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
