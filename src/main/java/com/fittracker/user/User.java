package com.fittracker.user;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Modele applicatif User (Phase 3, in-memory). Sera annote @Entity en Phase 4 sans changer la
 * surface publique.
 */
public final class User {
  private UUID id;
  private String email;
  private String passwordHash;
  private String displayName;
  private OffsetDateTime createdAt;
  private OffsetDateTime deletedAt;

  public User() {}

  public User(UUID id, String email, String passwordHash, String displayName, OffsetDateTime createdAt) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(OffsetDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  public boolean isActive() {
    return deletedAt == null;
  }
}
