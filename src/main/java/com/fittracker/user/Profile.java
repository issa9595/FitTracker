package com.fittracker.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Entite JPA Profile. Relation One-to-One avec {@link User} partageant la cle primaire via {@link
 * MapsId} : la PK de profiles est aussi la FK vers users.
 *
 * <p>La cle primaire etant assignee (et non generee), {@link Persistable} est implemente pour que
 * Spring Data sache distinguer une creation d'une mise a jour. Sans cela, un {@code userId} deja
 * renseigne ferait croire au repository que l'entite existe deja, declenchant un {@code merge} qui
 * echoue sur une ligne {@code @MapsId} encore absente (AssertionFailure: null identifier).
 */
@Entity
@Table(name = "profiles")
public class Profile implements Persistable<UUID> {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  @Transient private boolean persisted;

  @Column(name = "height_cm")
  private Integer heightCm;

  @Column(name = "weight_kg")
  private Double weightKg;

  @Column(name = "goal_weight_kg")
  private Double goalWeightKg;

  @Column(length = 500)
  private String bio;

  public Profile() {}

  public Profile(UUID userId, Integer heightCm, Double weightKg, Double goalWeightKg, String bio) {
    this.userId = userId;
    this.heightCm = heightCm;
    this.weightKg = weightKg;
    this.goalWeightKg = goalWeightKg;
    this.bio = bio;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Integer getHeightCm() {
    return heightCm;
  }

  public void setHeightCm(Integer heightCm) {
    this.heightCm = heightCm;
  }

  public Double getWeightKg() {
    return weightKg;
  }

  public void setWeightKg(Double weightKg) {
    this.weightKg = weightKg;
  }

  public Double getGoalWeightKg() {
    return goalWeightKg;
  }

  public void setGoalWeightKg(Double goalWeightKg) {
    this.goalWeightKg = goalWeightKg;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  @Override
  public UUID getId() {
    return userId;
  }

  /** Nouvelle tant que l'entite n'a pas ete persistee ou chargee depuis la base. */
  @Override
  public boolean isNew() {
    return !persisted;
  }

  @PostPersist
  @PostLoad
  void markPersisted() {
    this.persisted = true;
  }
}
