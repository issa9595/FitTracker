package com.fittracker.training;

import com.fittracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Entite JPA Program (N-1 User). */
@Entity
@Table(name = "programs")
@EntityListeners(AuditingEntityListener.class)
public class Program {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 2000)
  private String description;

  @Column(name = "target_metric", length = 120)
  private String targetMetric;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Version private long version;

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

  public User getUser() {
    return user;
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

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
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
