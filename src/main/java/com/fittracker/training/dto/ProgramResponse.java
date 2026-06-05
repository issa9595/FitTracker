package com.fittracker.training.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.hateoas.RepresentationModel;

@Schema(description = "Programme d'entrainement")
public class ProgramResponse extends RepresentationModel<ProgramResponse> {
  private UUID id;
  private UUID userId;
  private String name;
  private String description;
  private String targetMetric;
  private LocalDate startDate;
  private LocalDate endDate;
  private OffsetDateTime createdAt;

  public ProgramResponse() {}

  public ProgramResponse(
      UUID id,
      UUID userId,
      String name,
      String description,
      String targetMetric,
      LocalDate startDate,
      LocalDate endDate,
      OffsetDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.name = name;
    this.description = description;
    this.targetMetric = targetMetric;
    this.startDate = startDate;
    this.endDate = endDate;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getTargetMetric() {
    return targetMetric;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
