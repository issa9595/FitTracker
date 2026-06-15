package com.fittracker.user;

import com.fittracker.common.error.NotFoundException;
import com.fittracker.user.dto.ProfileUpdateRequest;
import com.fittracker.user.dto.UserUpdateRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;

  public UserService(UserRepository userRepository, ProfileRepository profileRepository) {
    this.userRepository = userRepository;
    this.profileRepository = profileRepository;
  }

  @Transactional(readOnly = true)
  public User getById(UUID id) {
    return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
  }

  @Transactional
  public User updateUser(UUID id, UserUpdateRequest update) {
    User user = getById(id);
    if (update.displayName() != null) {
      user.setDisplayName(update.displayName());
    }
    return userRepository.save(user);
  }

  @Transactional(readOnly = true)
  public Profile getProfile(UUID userId) {
    if (!userRepository.existsById(userId)) {
      throw new NotFoundException("User", userId);
    }
    return profileRepository
        .findById(userId)
        .orElseGet(() -> new Profile(userId, null, null, null, null));
  }

  @Transactional
  public Profile updateProfile(UUID userId, ProfileUpdateRequest update) {
    if (!userRepository.existsById(userId)) {
      throw new NotFoundException("User", userId);
    }
    Profile profile =
        profileRepository
            .findById(userId)
            .orElseGet(() -> new Profile(userId, null, null, null, null));
    // @MapsId : l'association User doit etre renseignee pour deriver la PK partagee.
    if (profile.getUser() == null) {
      profile.setUser(userRepository.getReferenceById(userId));
    }
    if (update.heightCm() != null) {
      profile.setHeightCm(update.heightCm());
    }
    if (update.weightKg() != null) {
      profile.setWeightKg(update.weightKg());
    }
    if (update.goalWeightKg() != null) {
      profile.setGoalWeightKg(update.goalWeightKg());
    }
    if (update.bio() != null) {
      profile.setBio(update.bio());
    }
    return profileRepository.save(profile);
  }
}
