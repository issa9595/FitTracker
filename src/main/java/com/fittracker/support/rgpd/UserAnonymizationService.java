package com.fittracker.support.rgpd;

import com.fittracker.notification.NotificationRepository;
import com.fittracker.social.FollowRepository;
import com.fittracker.training.TrainingSessionRepository;
import com.fittracker.user.ProfileRepository;
import com.fittracker.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline d'anonymisation RGPD declenche par {@code DELETE /api/v1/users/me}. Les sessions sont
 * reassignees au user sentinelle (conserve les agregats stats), puis follows, notifications et
 * profil sont supprimes, et le user est soft-supprime.
 */
@Service
public class UserAnonymizationService {

  /** User sentinelle "deleted" seede par la migration V3 (distinct du user de test ...001). */
  public static final UUID DELETED_SENTINEL_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final UserRepository users;
  private final ProfileRepository profiles;
  private final FollowRepository follows;
  private final NotificationRepository notifications;
  private final TrainingSessionRepository sessions;

  @PersistenceContext private EntityManager em;

  public UserAnonymizationService(
      UserRepository users,
      ProfileRepository profiles,
      FollowRepository follows,
      NotificationRepository notifications,
      TrainingSessionRepository sessions) {
    this.users = users;
    this.profiles = profiles;
    this.follows = follows;
    this.notifications = notifications;
    this.sessions = sessions;
  }

  @Transactional
  public void anonymize(UUID userId) {
    // 1. Reassigner les training_sessions au user sentinelle (conserve les agregats).
    em.createNativeQuery("UPDATE training_sessions SET user_id = :sentinel WHERE user_id = :uid")
        .setParameter("sentinel", DELETED_SENTINEL_ID)
        .setParameter("uid", userId)
        .executeUpdate();

    // 2. Supprimer les follows entrants et sortants.
    follows.deleteByIdFollowerIdOrIdFolloweeId(userId, userId);

    // 3. Supprimer les notifications.
    em.createNativeQuery("DELETE FROM notifications WHERE user_id = :uid")
        .setParameter("uid", userId)
        .executeUpdate();

    // 4. Supprimer le profil s'il existe.
    profiles.findById(userId).ifPresent(profiles::delete);

    // 5. Soft-delete du user (via @SQLDelete).
    users.findById(userId).ifPresent(users::delete);
  }
}
