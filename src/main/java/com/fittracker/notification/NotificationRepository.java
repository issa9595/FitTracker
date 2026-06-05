package com.fittracker.notification;

import com.fittracker.common.repository.InMemoryRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository extends InMemoryRepository<Notification, UUID> {
  @Override
  protected UUID idOf(Notification entity) {
    return entity.getId();
  }

  /** Renvoie les notifications du user les plus recentes en premier (createdAt desc, id desc). */
  public List<Notification> findByUserOrderByRecent(UUID userId) {
    return stream()
        .filter(n -> userId.equals(n.getUserId()))
        .sorted(
            Comparator.comparing(Notification::getCreatedAt)
                .thenComparing(Notification::getId)
                .reversed())
        .toList();
  }
}
