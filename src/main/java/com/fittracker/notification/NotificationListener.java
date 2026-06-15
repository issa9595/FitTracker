package com.fittracker.notification;

import com.fittracker.notification.event.NewPrEvent;
import com.fittracker.notification.event.SessionCompletedEvent;
import com.fittracker.social.Follow;
import com.fittracker.social.FollowRepository;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reagit aux evenements metier APRES commit de la transaction d'origine : persiste la notification
 * puis la pousse en temps reel sur {@code /topic/notifications/{userId}}.
 */
@Component
public class NotificationListener {

  private static final Logger LOG = LoggerFactory.getLogger(NotificationListener.class);
  private static final String DESTINATION_PREFIX = "/topic/notifications/";

  private final FollowRepository followRepository;
  private final NotificationService notificationService;
  private final SimpMessagingTemplate messaging;

  public NotificationListener(
      FollowRepository followRepository,
      NotificationService notificationService,
      SimpMessagingTemplate messaging) {
    this.followRepository = followRepository;
    this.notificationService = notificationService;
    this.messaging = messaging;
  }

  /** Une seance terminee notifie chaque follower de l'auteur. */
  @TransactionalEventListener
  public void onSessionCompleted(SessionCompletedEvent event) {
    for (Follow follow : followRepository.findByIdFolloweeId(event.actorUserId())) {
      UUID followerId = follow.getFollowerId();
      Map<String, Object> payload =
          Map.of(
              "actorUserId", event.actorUserId().toString(),
              "sessionId", event.sessionId().toString());
      Notification notif =
          notificationService.create(
              followerId, NotificationType.FRIEND_SESSION_COMPLETED, payload);
      push(followerId, notif);
    }
  }

  /** Un nouveau record personnel notifie l'utilisateur lui-meme. */
  @TransactionalEventListener
  public void onNewPr(NewPrEvent event) {
    Map<String, Object> payload =
        Map.of("exerciseId", event.exerciseId().toString(), "value", event.value());
    Notification notif = notificationService.create(event.userId(), NotificationType.NEW_PR, payload);
    push(event.userId(), notif);
  }

  private void push(UUID userId, Notification notif) {
    LOG.debug("Push notification {} a l'utilisateur {}", notif.getType(), userId);
    messaging.convertAndSend(DESTINATION_PREFIX + userId, NotificationMessage.from(notif));
  }
}
