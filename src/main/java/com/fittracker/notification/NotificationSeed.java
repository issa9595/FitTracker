package com.fittracker.notification;

import com.fittracker.common.security.CurrentUserProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seed dev/test de notifications de demo. {@code @DependsOn("userSeed")} garantit que le user de
 * test (et donc la FK notifications.user_id) existe avant l'insertion.
 */
@Component
@Profile({"dev", "test"})
@DependsOn("userSeed")
public class NotificationSeed {

  private final NotificationService service;
  private final CurrentUserProvider currentUser;

  public NotificationSeed(NotificationService service, CurrentUserProvider currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @PostConstruct
  public void seed() {
    service.seedDemo(currentUser.currentUserId());
  }
}
