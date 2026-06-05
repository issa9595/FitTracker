package com.fittracker.training;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class Program {
  private UUID id;
  private UUID userId;
  private String name;
  private String description;
  private String targetMetric;
  private LocalDate startDate;
  private LocalDate endDate;
  private OffsetDateTime createdAt;

  public Program() {}

  public Program(
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

  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setTargetMetric(String targetMetric) {
    this.targetMetric = targetMetric;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }
}
