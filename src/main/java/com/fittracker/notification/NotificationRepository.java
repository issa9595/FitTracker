package com.fittracker.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  /** Notifications du user, plus recentes d'abord (createdAt desc, id desc en tie-break). */
  List<Notification> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId);

  List<Notification> findByUserId(UUID userId);
}
