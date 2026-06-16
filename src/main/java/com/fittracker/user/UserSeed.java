package com.fittracker.user;

import com.fittracker.common.security.CurrentUserProvider;
import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Seed dev/test : cree le user "test" reference par CurrentUserProvider afin que les endpoints
 * /users/me fonctionnent et que le login de demo soit possible. Le mot de passe est hache BCrypt
 * (meme encodeur que la prod). Desactive en prod (profil exclu).
 */
@Component
@org.springframework.context.annotation.Profile({"dev", "test"})
public class UserSeed {

  /** Mot de passe en clair du user de demo (profils dev/test uniquement). */
  public static final String TEST_USER_PASSWORD = "ChangeMe123!";

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final PasswordEncoder passwordEncoder;
  private final TransactionTemplate transactionTemplate;

  public UserSeed(
      UserRepository userRepository,
      ProfileRepository profileRepository,
      PasswordEncoder passwordEncoder,
      PlatformTransactionManager transactionManager) {
    this.userRepository = userRepository;
    this.profileRepository = profileRepository;
    this.passwordEncoder = passwordEncoder;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  /**
   * {@code @Transactional} ne s'applique pas sur un {@code @PostConstruct} (pas de proxy sur
   * l'auto-invocation de cycle de vie). On ouvre donc une transaction programmatique pour que les
   * deux insertions partagent la meme session : le user n'est insere qu'une fois et le profil
   * {@code @MapsId} lit l'instance geree.
   */
  @PostConstruct
  public void seed() {
    transactionTemplate.executeWithoutResult(status -> doSeed());
  }

  private void doSeed() {
    if (userRepository.existsById(CurrentUserProvider.TEST_USER_ID)) {
      return;
    }
    User test =
        userRepository.save(
            new User(
                CurrentUserProvider.TEST_USER_ID,
                "test@fittracker.dev",
                passwordEncoder.encode(TEST_USER_PASSWORD),
                "Test User",
                OffsetDateTime.now()));

    Profile profile = new Profile(test.getId(), 178, 75.0, 72.0, "User de demo (Phase 3)");
    profile.setUser(test); // @MapsId : association requise (instance geree retournee par save)
    profileRepository.save(profile);
  }
}
