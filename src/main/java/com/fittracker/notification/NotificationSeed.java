package com.fittracker.notification;

import com.fittracker.common.security.CurrentUserProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
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
