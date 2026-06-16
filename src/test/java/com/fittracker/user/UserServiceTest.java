package com.fittracker.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fittracker.common.error.NotFoundException;
import com.fittracker.user.dto.ProfileUpdateRequest;
import com.fittracker.user.dto.UserUpdateRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests unitaires (Mockito + AssertJ) du {@link UserService}. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileRepository profileRepository;

  private UserService userService;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, profileRepository);
  }

  @Test
  void should_return_user_when_found() {
    User user = new User(userId, "jane@fit.io", "hash", "Jane", null);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThat(userService.getById(userId)).isSameAs(user);
  }

  @Test
  void should_throw_not_found_when_user_absent() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getById(userId)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void should_update_display_name_when_provided() {
    User user = new User(userId, "jane@fit.io", "hash", "Jane", null);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User updated = userService.updateUser(userId, new UserUpdateRequest("Jane Doe"));

    assertThat(updated.getDisplayName()).isEqualTo("Jane Doe");
    verify(userRepository).save(user);
  }

  @Test
  void should_keep_display_name_when_update_is_null() {
    User user = new User(userId, "jane@fit.io", "hash", "Jane", null);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User updated = userService.updateUser(userId, new UserUpdateRequest(null));

    assertThat(updated.getDisplayName()).isEqualTo("Jane");
  }

  @Test
  void should_return_existing_profile_when_present() {
    Profile profile = new Profile(userId, 180, 75.0, 72.0, "bio");
    when(userRepository.existsById(userId)).thenReturn(true);
    when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

    assertThat(userService.getProfile(userId)).isSameAs(profile);
  }

  @Test
  void should_return_empty_profile_when_absent() {
    when(userRepository.existsById(userId)).thenReturn(true);
    when(profileRepository.findById(userId)).thenReturn(Optional.empty());

    Profile profile = userService.getProfile(userId);

    assertThat(profile.getUserId()).isEqualTo(userId);
    assertThat(profile.getHeightCm()).isNull();
  }

  @Test
  void should_throw_not_found_when_getting_profile_of_unknown_user() {
    when(userRepository.existsById(userId)).thenReturn(false);

    assertThatThrownBy(() -> userService.getProfile(userId))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void should_throw_not_found_when_updating_profile_of_unknown_user() {
    when(userRepository.existsById(userId)).thenReturn(false);

    assertThatThrownBy(
            () -> userService.updateProfile(userId, new ProfileUpdateRequest(180, 75.0, 72.0, "x")))
        .isInstanceOf(NotFoundException.class);
    verify(profileRepository, never()).save(any());
  }

  @Test
  void should_create_profile_and_link_user_when_absent() {
    when(userRepository.existsById(userId)).thenReturn(true);
    when(profileRepository.findById(userId)).thenReturn(Optional.empty());
    when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

    Profile saved =
        userService.updateProfile(userId, new ProfileUpdateRequest(182, 80.0, 75.0, "athlete"));

    assertThat(saved.getHeightCm()).isEqualTo(182);
    assertThat(saved.getWeightKg()).isEqualTo(80.0);
    assertThat(saved.getGoalWeightKg()).isEqualTo(75.0);
    assertThat(saved.getBio()).isEqualTo("athlete");
    verify(userRepository).getReferenceById(userId);
  }

  @Test
  void should_update_existing_profile_fields_when_present() {
    Profile profile = new Profile(userId, 170, 70.0, 68.0, "old");
    profile.setUser(new User(userId, "jane@fit.io", "hash", "Jane", null));
    when(userRepository.existsById(userId)).thenReturn(true);
    when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
    when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

    Profile saved =
        userService.updateProfile(userId, new ProfileUpdateRequest(190, null, null, "new bio"));

    assertThat(saved.getHeightCm()).isEqualTo(190);
    assertThat(saved.getWeightKg()).isEqualTo(70.0);
    assertThat(saved.getBio()).isEqualTo("new bio");
    verify(userRepository, never()).getReferenceById(any());
  }
}
