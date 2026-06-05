package com.fittracker.user;

import com.fittracker.common.security.CurrentUserProvider;
import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/**
 * Seed minimal pour la Phase 3 : cree le user "test" referenced par CurrentUserProvider afin que
 * les endpoints /users/me puissent fonctionner sans qu'il faille passer par /auth/register.
 * Cette classe sera retiree en Phase 6 quand l'auth reelle sera en place.
 */
@Component
public class UserSeed {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;

  public UserSeed(UserRepository userRepository, ProfileRepository profileRepository) {
    this.userRepository = userRepository;
    this.profileRepository = profileRepository;
  }

  @PostConstruct
  public void seed() {
    if (userRepository.existsById(CurrentUserProvider.TEST_USER_ID)) {
      return;
    }
    User test =
        new User(
            CurrentUserProvider.TEST_USER_ID,
            "test@fittracker.dev",
            "$2a$12$disabled-phase-6-real-hash",
            "Test User",
            OffsetDateTime.now());
    userRepository.save(test);
    profileRepository.save(new Profile(test.getId(), 178, 75.0, 72.0, "User de demo (Phase 3)"));
  }
}
