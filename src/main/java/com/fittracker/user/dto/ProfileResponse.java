package com.fittracker.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import org.springframework.hateoas.RepresentationModel;

@Schema(description = "Profil sportif")
public class ProfileResponse extends RepresentationModel<ProfileResponse> {
  private UUID userId;
  private Integer heightCm;
  private Double weightKg;
  private Double goalWeightKg;
  private String bio;

  public ProfileResponse() {}

  public ProfileResponse(
      UUID userId, Integer heightCm, Double weightKg, Double goalWeightKg, String bio) {
    this.userId = userId;
    this.heightCm = heightCm;
    this.weightKg = weightKg;
    this.goalWeightKg = goalWeightKg;
    this.bio = bio;
  }

  public UUID getUserId() {
    return userId;
  }

  public Integer getHeightCm() {
    return heightCm;
  }

  public Double getWeightKg() {
    return weightKg;
  }

  public Double getGoalWeightKg() {
    return goalWeightKg;
  }

  public String getBio() {
    return bio;
  }
}
