package com.fittracker.training.mapper;

import com.fittracker.training.Program;
import com.fittracker.training.dto.ProgramResponse;
import org.springframework.stereotype.Component;

@Component
public class ProgramMapper {

  public ProgramResponse toResponse(Program p) {
    return new ProgramResponse(
        p.getId(),
        p.getUserId(),
        p.getName(),
        p.getDescription(),
        p.getTargetMetric(),
        p.getStartDate(),
        p.getEndDate(),
        p.getCreatedAt());
  }
}
