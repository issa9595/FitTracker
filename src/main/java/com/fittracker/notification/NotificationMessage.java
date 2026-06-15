package com.fittracker.notification;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Charge utile poussee aux clients WebSocket. DTO plat volontairement distinct de {@code
 * NotificationResponse} (qui porte des liens HATEOAS necessitant un contexte de requete HTTP, absent
 * dans le thread d'envoi WebSocket).
 */
public record NotificationMessage(
    UUID id, NotificationType type, Map<String, Object> payload, OffsetDateTime createdAt) {

  public static NotificationMessage from(Notification n) {
    return new NotificationMessage(n.getId(), n.getType(), n.getPayload(), n.getCreatedAt());
  }
}
