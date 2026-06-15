package com.fittracker.notification;

import com.fittracker.common.error.ForbiddenException;
import com.fittracker.common.error.NotFoundException;
import com.fittracker.common.pagination.CursorEncoder;
import com.fittracker.common.pagination.CursorPageRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final NotificationRepository repository;
  private final CursorEncoder cursorEncoder;

  public NotificationService(NotificationRepository repository, CursorEncoder cursorEncoder) {
    this.repository = repository;
    this.cursorEncoder = cursorEncoder;
  }

  /** Renvoie une page paginee par curseur. Le curseur encode l'id de la derniere notif lue. */
  @Transactional(readOnly = true)
  public CursorSlice list(UUID userId, CursorPageRequest pageRequest) {
    List<Notification> all = repository.findByUserIdOrderByCreatedAtDescIdDesc(userId);
    String afterIdRaw = cursorEncoder.decode(pageRequest.cursor());
    UUID afterId = afterIdRaw == null ? null : UUID.fromString(afterIdRaw);

    int startIndex = 0;
    if (afterId != null) {
      for (int i = 0; i < all.size(); i++) {
        if (all.get(i).getId().equals(afterId)) {
          startIndex = i + 1;
          break;
        }
      }
    }
    int endExclusive = Math.min(startIndex + pageRequest.size(), all.size());
    List<Notification> slice = all.subList(startIndex, endExclusive);
    String nextCursor =
        endExclusive < all.size()
            ? cursorEncoder.encode(slice.get(slice.size() - 1).getId().toString())
            : null;
    return new CursorSlice(slice, nextCursor);
  }

  @Transactional
  public Notification markAsRead(UUID notificationId, UUID userId) {
    Notification n =
        repository
            .findById(notificationId)
            .orElseThrow(() -> new NotFoundException("Notification", notificationId));
    if (!n.getUserId().equals(userId)) {
      throw new ForbiddenException("Cette notification ne vous appartient pas");
    }
    if (n.getReadAt() == null) {
      n.setReadAt(OffsetDateTime.now());
      repository.save(n);
    }
    return n;
  }

  /** Pousse des notifications de demo (seed dev/test). Sera remplace en phase 5 par l'evenementiel. */
  @Transactional
  public void seedDemo(UUID userId) {
    if (!repository.findByUserId(userId).isEmpty()) {
      return;
    }
    repository.save(
        new Notification(
            UUID.randomUUID(),
            userId,
            NotificationType.ACHIEVEMENT,
            Map.of("badge", "first-week-streak"),
            OffsetDateTime.now()));
    repository.save(
        new Notification(
            UUID.randomUUID(),
            userId,
            NotificationType.NEW_PR,
            Map.of("exercise", "squat", "weight", 110.0),
            OffsetDateTime.now()));
  }

  public record CursorSlice(List<Notification> content, String nextCursor) {}
}
