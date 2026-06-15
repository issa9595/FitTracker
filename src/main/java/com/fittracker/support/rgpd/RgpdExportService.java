package com.fittracker.support.rgpd;

import com.fittracker.common.error.NotFoundException;
import com.fittracker.notification.Notification;
import com.fittracker.notification.NotificationRepository;
import com.fittracker.social.Follow;
import com.fittracker.social.FollowRepository;
import com.fittracker.training.Program;
import com.fittracker.training.ProgramRepository;
import com.fittracker.training.SessionExercise;
import com.fittracker.training.TrainingSession;
import com.fittracker.training.TrainingSessionRepository;
import com.fittracker.user.Profile;
import com.fittracker.user.ProfileRepository;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agrege l'integralite des donnees d'un utilisateur pour le droit a la portabilite (RGPD). Construit
 * des Map de valeurs scalaires DANS la transaction (open-in-view=false) : aucune entite JPA n'est
 * serialisee directement et aucune association LAZY n'est exposee a Jackson.
 */
@Service
public class RgpdExportService {

  private final UserRepository users;
  private final ProfileRepository profiles;
  private final TrainingSessionRepository sessions;
  private final ProgramRepository programs;
  private final FollowRepository follows;
  private final NotificationRepository notifications;

  public RgpdExportService(
      UserRepository users,
      ProfileRepository profiles,
      TrainingSessionRepository sessions,
      ProgramRepository programs,
      FollowRepository follows,
      NotificationRepository notifications) {
    this.users = users;
    this.profiles = profiles;
    this.sessions = sessions;
    this.programs = programs;
    this.follows = follows;
    this.notifications = notifications;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> exportFor(UUID userId) {
    User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("user", userMap(user));
    result.put("profile", profiles.findById(userId).map(this::profileMap).orElse(null));
    result.put("trainingSessions", sessions.findByUserId(userId).stream().map(this::sessionMap).toList());
    result.put("programs", programs.findByUserId(userId).stream().map(this::programMap).toList());
    result.put("followers", follows.findByIdFolloweeId(userId).stream().map(this::followMap).toList());
    result.put("following", follows.findByIdFollowerId(userId).stream().map(this::followMap).toList());
    result.put(
        "notifications", notifications.findByUserId(userId).stream().map(this::notificationMap).toList());
    return result;
  }

  private Map<String, Object> userMap(User u) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", u.getId());
    m.put("email", u.getEmail());
    m.put("displayName", u.getDisplayName());
    m.put("createdAt", u.getCreatedAt());
    return m;
  }

  private Map<String, Object> profileMap(Profile p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("heightCm", p.getHeightCm());
    m.put("weightKg", p.getWeightKg());
    m.put("goalWeightKg", p.getGoalWeightKg());
    m.put("bio", p.getBio());
    return m;
  }

  private Map<String, Object> sessionMap(TrainingSession s) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", s.getId());
    m.put("startedAt", s.getStartedAt());
    m.put("durationSeconds", s.getDurationSeconds());
    m.put("type", s.getType());
    m.put("notes", s.getNotes());
    List<Map<String, Object>> exercises = new ArrayList<>();
    for (SessionExercise e : s.getExercises()) {
      Map<String, Object> em = new LinkedHashMap<>();
      em.put("exerciseId", e.getExerciseId());
      em.put("position", e.getPosition());
      em.put("sets", e.getSets());
      em.put("reps", e.getReps());
      em.put("weightKg", e.getWeightKg());
      em.put("distanceM", e.getDistanceM());
      em.put("timeSeconds", e.getTimeSeconds());
      exercises.add(em);
    }
    m.put("exercises", exercises);
    return m;
  }

  private Map<String, Object> programMap(Program p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", p.getId());
    m.put("name", p.getName());
    m.put("description", p.getDescription());
    m.put("targetMetric", p.getTargetMetric());
    m.put("startDate", p.getStartDate());
    m.put("endDate", p.getEndDate());
    return m;
  }

  private Map<String, Object> followMap(Follow f) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("followerId", f.getFollowerId());
    m.put("followeeId", f.getFolloweeId());
    m.put("createdAt", f.getCreatedAt());
    return m;
  }

  private Map<String, Object> notificationMap(Notification n) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", n.getId());
    m.put("type", n.getType());
    m.put("payload", n.getPayload());
    m.put("readAt", n.getReadAt());
    m.put("createdAt", n.getCreatedAt());
    return m;
  }
}
