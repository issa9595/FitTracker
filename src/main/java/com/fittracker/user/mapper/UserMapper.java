package com.fittracker.user.mapper;

import com.fittracker.user.Profile;
import com.fittracker.user.User;
import com.fittracker.user.dto.ProfileResponse;
import com.fittracker.user.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserResponse toResponse(User user) {
    if (user == null) {
      return null;
    }
    return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
  }

  public ProfileResponse toResponse(Profile profile) {
    if (profile == null) {
      return null;
    }
    return new ProfileResponse(
        profile.getUserId(),
        profile.getHeightCm(),
        profile.getWeightKg(),
        profile.getGoalWeightKg(),
        profile.getBio());
  }
}
