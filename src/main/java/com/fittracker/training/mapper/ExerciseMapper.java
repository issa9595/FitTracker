package com.fittracker.training.mapper;

import com.fittracker.training.Exercise;
import com.fittracker.training.dto.ExerciseResponse;
import org.springframework.stereotype.Component;

@Component
public class ExerciseMapper {

  public ExerciseResponse toResponse(Exercise e) {
    return new ExerciseResponse(e.getId(), e.getName(), e.getCategory(), e.getMuscleGroup(), e.getUnit());
  }
}
