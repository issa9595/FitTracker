package com.fittracker.training.dto;

import com.fittracker.training.ExerciseCategory;
import com.fittracker.training.ExerciseUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import org.springframework.hateoas.RepresentationModel;

@Schema(description = "Exercice du referentiel")
public class ExerciseResponse extends RepresentationModel<ExerciseResponse> {
  private UUID id;
  private String name;
  private ExerciseCategory category;
  private String muscleGroup;
  private ExerciseUnit unit;

  public ExerciseResponse() {}

  public ExerciseResponse(
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
}
