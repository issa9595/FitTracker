package com.fittracker.notification.event;

import java.util.UUID;

/** Emis quand un utilisateur termine (cree) une seance : notifie ses followers. */
public record SessionCompletedEvent(UUID actorUserId, UUID sessionId) {}
