package com.fittracker.notification.event;

import java.util.UUID;

/** Emis quand un utilisateur bat son record de charge sur un exercice (Personal Record). */
public record NewPrEvent(UUID userId, UUID exerciseId, double value) {}
