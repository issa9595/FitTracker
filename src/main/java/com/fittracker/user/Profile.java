package com.fittracker.user;

import java.util.UUID;

public final class Profile {
  private UUID userId;
  private Integer heightCm;
  private Double weightKg;
  private Double goalWeightKg;
  private String bio;

  public Profile() {}

  public Profile(UUID userId, Integer heightCm, Double weightKg, Double goalWeightKg, String bio) {
    this.userId = userId;
    this.heightCm = heightCm;
    this.weightKg = weightKg;
    this.goalWeightKg = goalWeightKg;
    this.bio = bio;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public Integer getHeightCm() {
    return heightCm;
  }

  public void setHeightCm(Integer heightCm) {
    this.heightCm = heightCm;
  }

  public Double getWeightKg() {
    return weightKg;
  }

  public void setWeightKg(Double weightKg) {
    this.weightKg = weightKg;
  }

  public Double getGoalWeightKg() {
    return goalWeightKg;
  }

  public void setGoalWeightKg(Double goalWeightKg) {
    this.goalWeightKg = goalWeightKg;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }
}
