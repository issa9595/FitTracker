package com.fittracker.notification;

import com.fittracker.common.security.CurrentUserProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seed dev/test de notifications de demo. {@code @DependsOn("userSeed")} garantit que le user de
 * test (et donc la FK notifications.user_id) existe avant l'insertion.
 *
 * <p>Le seed s'execute au demarrage, hors requete HTTP : il n'y a pas de {@code SecurityContext}.
 * On cible donc directement {@link CurrentUserProvider#TEST_USER_ID} (et non
 * {@code currentUserId()} qui exige une authentification depuis la Phase 6).
 */
@Component
@Profile({"dev", "test"})
@DependsOn("userSeed")
public class NotificationSeed {

  private final NotificationService service;

  public NotificationSeed(NotificationService service) {
    this.service = service;
  }

  @PostConstruct
  public void seed() {
    service.seedDemo(CurrentUserProvider.TEST_USER_ID);
  }
}
