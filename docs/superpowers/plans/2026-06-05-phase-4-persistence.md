# Phase 4 — Persistence ORM, relations, CRUD, RGPD — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remplacer la couche `InMemoryRepository` de la Phase 3 par une persistence JPA/Hibernate sur PostgreSQL 16, géré par Flyway, avec audit, soft-delete RGPD, optimistic locking et pipeline d'anonymisation utilisateur. Démontrer les 4 types de relations exigés par le livrable 4 via des tests d'intégration Testcontainers.

**Architecture :**
- Migrations Flyway (`V1__init.sql` schéma, `V2__seed_exercises.sql` référentiel, `V3__seed_deleted_user.sql` sentinelle anonymisation).
- Entités existantes (POJOs Phase 3, surface publique stable) annotées `@Entity` sans rupture de DTO/controller.
- `InMemoryRepository` supprimé ; chaque repo devient un `JpaRepository` + `JpaSpecificationExecutor` quand le filtrage dynamique est requis.
- RGPD : `DELETE /api/v1/users/me` réassigne les `training_sessions` à un user sentinelle (`00000000-0000-0000-0000-000000000000`) puis cascade-delete profils/follows/notifs ; `GET /api/v1/users/me/export` agrège les données en JSON.
- Tests : `@SpringBootTest` + Testcontainers Postgres 16 via `@ServiceConnection` (Spring Boot 3.1+). Un test d'intégration par type de relation.

**Tech Stack :**
- Spring Boot 3.3.5 / Java 21 / Maven (existant)
- Ajouts : `spring-boot-starter-data-jpa`, `postgresql` (runtime), `flyway-core`, `flyway-database-postgresql`, `org.testcontainers:postgresql` + `org.testcontainers:junit-jupiter` (test), `org.mapstruct:mapstruct` (1.6.x) + annotation processor.
- Hibernate 6 (transitive) → JSONB natif via `@JdbcTypeCode(SqlTypes.JSON)` pour `notifications.payload`.

---

## Pre-flight check

État au démarrage de cette phase (vérifié le 2026-06-05, branche `feature/phase-4-persistence`) :
- Phase 3 mergée (PR #3 → main, commit `06eac24`). Working tree clean.
- POJOs Phase 3 ont des setters publics ; commentaire explicite `User.java:7-8` : « Sera annoté @Entity en Phase 4 sans changer la surface publique ».
- `application.yml:14-20` contient déjà la configuration `spring.datasource.*` (placeholders `${SPRING_DATASOURCE_*}`).
- `docker-compose.yml` (Phase 2) expose déjà `db` (Postgres 16) et `redis`.
- Pas encore de migrations dans `src/main/resources/db/migration/` (à créer).
- `docs/architecture.md` manquant (livrable phase 4).

---

## File Structure

**Nouvelles arborescences créées :**

```
src/main/resources/db/migration/
├── V1__init.sql                  # Schéma complet (8 tables + index + FK)
├── V2__seed_exercises.sql        # Référentiel exercices (12 lignes, UUID déterministes)
└── V3__seed_deleted_user.sql     # User sentinelle pour anonymisation RGPD

src/main/java/com/fittracker/support/
├── AuditingConfig.java           # @EnableJpaAuditing
├── JsonMapAttributeConverter.java # (si besoin) fallback si @JdbcTypeCode JSON ne suffit pas
└── rgpd/
    ├── RgpdExportService.java    # Agrège l'export JSON de l'utilisateur
    ├── RgpdExportController.java # GET /api/v1/users/me/export
    └── UserAnonymizationService.java # Pipeline DELETE /me

src/test/java/com/fittracker/support/
└── AbstractIntegrationTest.java  # @SpringBootTest + Testcontainers Postgres via @ServiceConnection

src/test/java/com/fittracker/persistence/
├── OneToOneRelationIT.java       # User ↔ Profile
├── OneToManyRelationIT.java      # User → TrainingSession + User → Notification
├── ManyToManyWithAttrsIT.java    # TrainingSession ↔ Exercise via SessionExercise
├── ManyToManySelfRefIT.java      # User ↔ User via Follow
└── RgpdPipelineIT.java           # DELETE /me + GET /me/export

docs/
└── architecture.md               # Schéma global + ERD Mermaid (livrable phase 4)

docs/superpowers/plans/
└── 2026-06-05-phase-4-persistence.md   # ce document
```

**Fichiers modifiés (entités → @Entity) :**

```
src/main/java/com/fittracker/
├── user/User.java                 # ajouter @Entity, @SoftDelete via deletedAt, @Version, audit
├── user/Profile.java              # @Entity + @OneToOne @MapsId vers User
├── training/Exercise.java         # @Entity simple (référentiel)
├── training/TrainingSession.java  # @Entity, @ManyToOne User, @OneToMany SessionExercise, @Version
├── training/SessionExercise.java  # @Entity, @EmbeddedId (sessionId, exerciseId, position) ou @IdClass
├── training/Program.java          # @Entity, @ManyToOne User, @Version
├── social/Follow.java             # @Entity, @EmbeddedId composite (followerId, followeeId)
├── notification/Notification.java # @Entity, payload JSONB via @JdbcTypeCode(SqlTypes.JSON)
└── common/repository/InMemoryRepository.java # SUPPRIMÉ après migration

src/main/java/com/fittracker/{user,training,social,notification}/*Repository.java
                                   # tous deviennent interfaces JpaRepository
src/main/java/com/fittracker/user/UserService.java
src/main/java/com/fittracker/user/UserSeed.java
src/main/java/com/fittracker/training/ExerciseSeed.java       # SUPPRIMÉ (remplacé par V2)
src/main/java/com/fittracker/notification/NotificationSeed.java # adapter ou supprimer
src/main/java/com/fittracker/training/TrainingSessionService.java
src/main/java/com/fittracker/training/ProgramService.java
src/main/java/com/fittracker/social/FollowService.java
src/main/java/com/fittracker/notification/NotificationService.java
src/main/java/com/fittracker/auth/AuthService.java
```

**Fichiers de config touchés :**

```
pom.xml                                       # +5 dépendances
src/main/resources/application.yml            # +spring.jpa.* (open-in-view=false), +spring.flyway.*
src/test/resources/application-test.yml       # NOUVEAU profil test
.github/workflows/ci.yml                      # ajouter step Testcontainers (Docker déjà dispo)
README.md                                     # section persistance / commandes Flyway
docker-compose.yml                            # rien à changer (postgres déjà OK)
```

---

## Conventions de commit (rappel brief §0)

Conventional Commits, un commit = un changement atomique. Format :
- `build(deps): ajouter JPA, Flyway, Postgres, Testcontainers`
- `feat(persistence): migration V1 schéma initial`
- `feat(user): annoter JPA + soft-delete`
- `feat(training): entités JPA + relations`
- `feat(social): Follow JPA composite key`
- `feat(notification): Notification JPA + payload JSONB`
- `refactor(repository): remplacer InMemoryRepository par JpaRepository`
- `feat(rgpd): export utilisateur + anonymisation DELETE /me`
- `test(persistence): testcontainers IT pour les 4 types de relations`
- `docs(architecture): ERD Mermaid + diagramme containers`
- `ci: activer Testcontainers dans la pipeline`

---

## Task 1: Ajouter les dépendances Maven

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1 — Ajouter les dépendances dans le bloc `<dependencies>` (après le starter-validation)**

Ajouter, dans l'ordre logique :

```xml
<!-- JPA + Hibernate (Phase 4) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Migrations Flyway -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>

<!-- PostgreSQL driver (runtime) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- MapStruct (mapping DTO sans réflexion) -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>

<!-- Testcontainers Postgres (tests d'intégration) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

Et dans `<properties>` :

```xml
<mapstruct.version>1.6.3</mapstruct.version>
```

- [ ] **Step 2 — Configurer le processeur d'annotations MapStruct**

Dans le `<build><plugins>` du `pom.xml`, sur le plugin `maven-compiler-plugin` (à ajouter si absent) :

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>21</source>
        <target>21</target>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

- [ ] **Step 3 — Vérifier la résolution**

Run: `mvn -q -DskipTests dependency:resolve`
Expected: `BUILD SUCCESS`, pas d'erreur de résolution.

- [ ] **Step 4 — Commit**

```bash
git add pom.xml
git commit -m "build(deps): add JPA, Flyway, PostgreSQL, Testcontainers, MapStruct"
```

---

## Task 2: Configurer JPA, Flyway et le profil de test

**Files:**
- Modify: `src/main/resources/application.yml`
- Create: `src/test/resources/application-test.yml`

- [ ] **Step 1 — Étendre `application.yml` avec la config JPA et Flyway**

Sous le bloc `spring:` existant, ajouter (à la suite de `datasource:`) :

```yaml
  jpa:
    hibernate:
      ddl-auto: validate          # Flyway gère le schéma. Jamais 'update' (cf. brief §6.6).
    open-in-view: false           # Anti-pattern : on ne garde pas la session ouverte côté MVC.
    properties:
      hibernate:
        format_sql: ${HIBERNATE_FORMAT_SQL:false}
        jdbc:
          time_zone: UTC
    show-sql: ${HIBERNATE_SHOW_SQL:false}

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
```

- [ ] **Step 2 — Créer `src/test/resources/application-test.yml`**

```yaml
# Profil test : Testcontainers fournit la datasource via @ServiceConnection.
# On garde Flyway actif (validation du schéma de bout en bout en CI).
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration
    clean-disabled: false
  data:
    redis:
      host: localhost
      port: 6379

logging:
  level:
    root: WARN
    com.fittracker: INFO
    org.hibernate.SQL: WARN
```

- [ ] **Step 3 — Commit**

```bash
git add src/main/resources/application.yml src/test/resources/application-test.yml
git commit -m "feat(persistence): wire JPA + Flyway config and test profile"
```

---

## Task 3: Migration Flyway V1 — schéma initial complet

**Files:**
- Create: `src/main/resources/db/migration/V1__init.sql`

- [ ] **Step 1 — Écrire la migration**

```sql
-- =====================================================================
-- FitTracker V1 — Schéma initial (Phase 4).
-- Couvre toutes les entités du brief §2 : User, Profile, Exercise,
-- TrainingSession, SessionExercise, Program, Follow, Notification.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------------
-- USERS (RGPD soft-delete via deleted_at)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(254) NOT NULL,
    password_hash   VARCHAR(120) NOT NULL,
    display_name    VARCHAR(80)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_users_email_active
    ON users (LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX ix_users_deleted_at ON users (deleted_at);

-- ---------------------------------------------------------------------
-- PROFILES (One-to-One avec User, PK = FK)
-- ---------------------------------------------------------------------
CREATE TABLE profiles (
    user_id         UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    height_cm       INTEGER,
    weight_kg       DOUBLE PRECISION,
    goal_weight_kg  DOUBLE PRECISION,
    bio             VARCHAR(500)
);

-- ---------------------------------------------------------------------
-- EXERCISES (référentiel partagé)
-- ---------------------------------------------------------------------
CREATE TABLE exercises (
    id              UUID PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    category        VARCHAR(20)  NOT NULL,
    muscle_group    VARCHAR(60)  NOT NULL,
    unit            VARCHAR(20)  NOT NULL
);
CREATE INDEX ix_exercises_category ON exercises (category);

-- ---------------------------------------------------------------------
-- TRAINING_SESSIONS (N-1 User)
-- ---------------------------------------------------------------------
CREATE TABLE training_sessions (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users(id),
    started_at          TIMESTAMPTZ NOT NULL,
    duration_seconds    INTEGER NOT NULL,
    type                VARCHAR(20) NOT NULL,
    notes               VARCHAR(2000),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_sessions_user_started ON training_sessions (user_id, started_at DESC);
CREATE INDEX ix_sessions_type ON training_sessions (type);

-- ---------------------------------------------------------------------
-- SESSION_EXERCISES (M-N TrainingSession ↔ Exercise avec attributs)
-- ---------------------------------------------------------------------
CREATE TABLE session_exercises (
    session_id      UUID NOT NULL REFERENCES training_sessions(id) ON DELETE CASCADE,
    exercise_id     UUID NOT NULL REFERENCES exercises(id),
    position        INTEGER NOT NULL,
    sets            INTEGER,
    reps            INTEGER,
    weight_kg       DOUBLE PRECISION,
    distance_m      INTEGER,
    time_seconds    INTEGER,
    PRIMARY KEY (session_id, exercise_id, position)
);
CREATE INDEX ix_session_exercises_exercise ON session_exercises (exercise_id);

-- ---------------------------------------------------------------------
-- PROGRAMS (N-1 User)
-- ---------------------------------------------------------------------
CREATE TABLE programs (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(2000),
    target_metric   VARCHAR(120),
    start_date      DATE,
    end_date        DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_programs_user ON programs (user_id);

-- ---------------------------------------------------------------------
-- FOLLOWS (M-N self-référençant User ↔ User)
-- ---------------------------------------------------------------------
CREATE TABLE follows (
    follower_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followee_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followee_id),
    CHECK (follower_id <> followee_id)
);
CREATE INDEX ix_follows_followee ON follows (followee_id);

-- ---------------------------------------------------------------------
-- NOTIFICATIONS (N-1 User, payload JSONB)
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(40) NOT NULL,
    payload         JSONB NOT NULL DEFAULT '{}'::jsonb,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX ix_notifications_unread ON notifications (user_id) WHERE read_at IS NULL;
```

- [ ] **Step 2 — Démarrer Postgres en local et vérifier la migration**

```bash
docker compose up -d db
mvn -q -DskipTests spring-boot:run
```

Expected (dans les logs) : `Successfully applied 1 migration to schema "public", now at version v1`.

Stopper l'app après la vérification (`Ctrl+C`).

- [ ] **Step 3 — Commit**

```bash
git add src/main/resources/db/migration/V1__init.sql
git commit -m "feat(persistence): V1 initial Flyway schema migration"
```

---

## Task 4: Migration Flyway V2 — référentiel d'exercices

**Files:**
- Create: `src/main/resources/db/migration/V2__seed_exercises.sql`
- Modify (delete): `src/main/java/com/fittracker/training/ExerciseSeed.java`

- [ ] **Step 1 — Écrire la migration de seed**

Conserver exactement les UUID déterministes de `ExerciseSeed.java:46-47` (`00000000-0000-0000-0000-0000000000<suffix>`) pour que les tests existants continuent à matcher.

```sql
-- =====================================================================
-- FitTracker V2 — Référentiel d'exercices (running, MMA, lutte,
-- musculation, mobilité). UUID déterministes pour stabilité des tests.
-- =====================================================================

INSERT INTO exercises (id, name, category, muscle_group, unit) VALUES
('00000000-0000-0000-0000-0000000000a1', 'Course a pied',         'RUNNING',   'cardio',    'DISTANCE'),
('00000000-0000-0000-0000-0000000000a2', 'Course fractionnee',    'RUNNING',   'cardio',    'TIME'),
('00000000-0000-0000-0000-0000000000b1', 'Developpe couche',      'STRENGTH',  'pectoraux', 'REPS'),
('00000000-0000-0000-0000-0000000000b2', 'Squat',                 'STRENGTH',  'jambes',    'REPS'),
('00000000-0000-0000-0000-0000000000b3', 'Souleve de terre',      'STRENGTH',  'dos',       'REPS'),
('00000000-0000-0000-0000-0000000000b4', 'Tractions',             'STRENGTH',  'dos',       'REPS'),
('00000000-0000-0000-0000-0000000000c1', 'Jab cross',             'MMA',       'full body', 'REPS'),
('00000000-0000-0000-0000-0000000000c2', 'Low kick',              'MMA',       'jambes',    'REPS'),
('00000000-0000-0000-0000-0000000000c3', 'Sac de frappe',         'MMA',       'full body', 'TIME'),
('00000000-0000-0000-0000-0000000000d1', 'Single leg takedown',   'WRESTLING', 'fullbody',  'REPS'),
('00000000-0000-0000-0000-0000000000d2', 'Sprawl',                'WRESTLING', 'full body', 'REPS'),
('00000000-0000-0000-0000-0000000000e1', 'Etirement',             'OTHER',     'mobilite',  'TIME');
```

- [ ] **Step 2 — Supprimer `ExerciseSeed.java`**

Run: `git rm src/main/java/com/fittracker/training/ExerciseSeed.java`

(La classe devient obsolète : Flyway sème le référentiel à l'init de la base, le `@PostConstruct` Java n'a plus de raison d'être.)

- [ ] **Step 3 — Vérifier**

```bash
docker compose down -v && docker compose up -d db
mvn -q -DskipTests spring-boot:run
```

Dans un autre terminal :

```bash
docker compose exec db psql -U fittracker -d fittracker -c "SELECT COUNT(*) FROM exercises;"
```

Expected: `12`.

Stopper l'app.

- [ ] **Step 4 — Commit**

```bash
git add src/main/resources/db/migration/V2__seed_exercises.sql src/main/java/com/fittracker/training/ExerciseSeed.java
git commit -m "feat(persistence): V2 seed exercises via Flyway, drop Java seed"
```

---

## Task 5: Migration V3 — user sentinelle pour anonymisation RGPD

**Files:**
- Create: `src/main/resources/db/migration/V3__seed_deleted_user.sql`

- [ ] **Step 1 — Écrire la migration**

Ce user sert de cible pour les FK des sessions anonymisées (cf. brief §2 règle 2 : « anonymise ses sessions mais conserve les agrégats stats »).

```sql
-- =====================================================================
-- FitTracker V3 — User sentinelle "deleted" pour RGPD.
-- Les training_sessions des utilisateurs supprimés sont réassignées
-- à ce user pour conserver les agrégats statistiques anonymement.
-- =====================================================================

INSERT INTO users (id, email, password_hash, display_name, created_at, updated_at, deleted_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'deleted-sentinel@fittracker.invalid',
    'N/A',
    '[deleted user]',
    now(),
    now(),
    now()  -- déjà soft-deleted : aucune route applicative ne le retourne
);
```

- [ ] **Step 2 — Vérifier**

```bash
docker compose down -v && docker compose up -d db
mvn -q -DskipTests spring-boot:run
```

```bash
docker compose exec db psql -U fittracker -d fittracker -c \
  "SELECT id, display_name FROM users WHERE id = '00000000-0000-0000-0000-000000000001';"
```

Expected: une ligne avec `[deleted user]`.

- [ ] **Step 3 — Commit**

```bash
git add src/main/resources/db/migration/V3__seed_deleted_user.sql
git commit -m "feat(rgpd): V3 seed sentinel user for session anonymization"
```

---

## Task 6: Convertir `User` et `Profile` en entités JPA

**Files:**
- Modify: `src/main/java/com/fittracker/user/User.java`
- Modify: `src/main/java/com/fittracker/user/Profile.java`
- Create: `src/main/java/com/fittracker/support/AuditingConfig.java`

- [ ] **Step 1 — Activer l'auditing JPA**

```java
package com.fittracker.support;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class AuditingConfig {}
```

- [ ] **Step 2 — Annoter `User` (conserver les getters/setters existants, ajouter `updatedAt`, `version`, et `@PrePersist`/`@PreUpdate` via audit)**

Remplacer la classe `User` par :

```java
package com.fittracker.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE users SET deleted_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class User {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @Version
  private long version;

  public User() {}

  public User(UUID id, String email, String passwordHash, String displayName, OffsetDateTime createdAt) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.createdAt = createdAt;
  }

  // garder tous les getters/setters existants de la version Phase 3
  // (id, email, passwordHash, displayName, createdAt, deletedAt)
  // + ajouter getter/setter pour updatedAt et version
  // ... (copier depuis l'ancienne classe, plus :)

  public OffsetDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
  public long getVersion() { return version; }

  public boolean isActive() { return deletedAt == null; }
}
```

Note importante : `@SQLDelete` rend le `delete()` JPA non-destructif. Pour réellement supprimer la ligne (RGPD complet du user sentinelle ou en tests), passer par du SQL natif via `@Modifying @Query`.

- [ ] **Step 3 — Annoter `Profile` (One-to-One avec User, PK partagée via `@MapsId`)**

Remplacer la classe `Profile` par :

```java
package com.fittracker.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

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

  // garder tous les getters/setters existants + ajouter pour user
  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }
  // ... (copier les autres de la version Phase 3)
}
```

- [ ] **Step 4 — Compiler**

Run: `mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 5 — Commit**

```bash
git add src/main/java/com/fittracker/user/User.java \
        src/main/java/com/fittracker/user/Profile.java \
        src/main/java/com/fittracker/support/AuditingConfig.java
git commit -m "feat(user): JPA entities with soft-delete, audit, optimistic locking"
```

---

## Task 7: Convertir `Exercise` en entité JPA

**Files:**
- Modify: `src/main/java/com/fittracker/training/Exercise.java`

- [ ] **Step 1 — Annoter `Exercise`**

```java
package com.fittracker.training;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class Exercise {

  @Id
  private UUID id;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ExerciseCategory category;

  @Column(name = "muscle_group", nullable = false, length = 60)
  private String muscleGroup;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ExerciseUnit unit;

  // garder le constructeur sans-arg + le constructeur complet + getters/setters
  // (copier depuis Phase 3)
}
```

- [ ] **Step 2 — Commit**

```bash
git add src/main/java/com/fittracker/training/Exercise.java
git commit -m "feat(training): annotate Exercise as JPA entity"
```

---

## Task 8: Convertir `TrainingSession` et `SessionExercise`

**Files:**
- Modify: `src/main/java/com/fittracker/training/TrainingSession.java`
- Modify: `src/main/java/com/fittracker/training/SessionExercise.java`
- Create: `src/main/java/com/fittracker/training/SessionExerciseId.java`

- [ ] **Step 1 — Créer la clé composite `SessionExerciseId`**

```java
package com.fittracker.training;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SessionExerciseId implements Serializable {

  @Column(name = "session_id")
  private UUID sessionId;

  @Column(name = "exercise_id")
  private UUID exerciseId;

  @Column(name = "position")
  private int position;

  public SessionExerciseId() {}

  public SessionExerciseId(UUID sessionId, UUID exerciseId, int position) {
    this.sessionId = sessionId;
    this.exerciseId = exerciseId;
    this.position = position;
  }

  public UUID getSessionId() { return sessionId; }
  public UUID getExerciseId() { return exerciseId; }
  public int getPosition() { return position; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SessionExerciseId other)) return false;
    return position == other.position
        && Objects.equals(sessionId, other.sessionId)
        && Objects.equals(exerciseId, other.exerciseId);
  }

  @Override
  public int hashCode() { return Objects.hash(sessionId, exerciseId, position); }
}
```

- [ ] **Step 2 — Annoter `SessionExercise`**

```java
package com.fittracker.training;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "session_exercises")
public class SessionExercise {

  @EmbeddedId
  private SessionExerciseId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("sessionId")
  @JoinColumn(name = "session_id")
  private TrainingSession session;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("exerciseId")
  @JoinColumn(name = "exercise_id")
  private Exercise exercise;

  private Integer sets;
  private Integer reps;

  @Column(name = "weight_kg")
  private Double weightKg;

  @Column(name = "distance_m")
  private Integer distanceM;

  @Column(name = "time_seconds")
  private Integer timeSeconds;

  public SessionExercise() {}

  public SessionExercise(UUID sessionId, UUID exerciseId, int position,
                         Integer sets, Integer reps, Double weightKg,
                         Integer distanceM, Integer timeSeconds) {
    this.id = new SessionExerciseId(sessionId, exerciseId, position);
    this.sets = sets;
    this.reps = reps;
    this.weightKg = weightKg;
    this.distanceM = distanceM;
    this.timeSeconds = timeSeconds;
  }

  public SessionExerciseId getId() { return id; }
  public UUID getSessionId() { return id != null ? id.getSessionId() : null; }
  public UUID getExerciseId() { return id != null ? id.getExerciseId() : null; }
  public int getPosition() { return id != null ? id.getPosition() : 0; }

  public Integer getSets() { return sets; }
  public Integer getReps() { return reps; }
  public Double getWeightKg() { return weightKg; }
  public Integer getDistanceM() { return distanceM; }
  public Integer getTimeSeconds() { return timeSeconds; }

  public TrainingSession getSession() { return session; }
  public void setSession(TrainingSession session) { this.session = session; }
  public Exercise getExercise() { return exercise; }
  public void setExercise(Exercise exercise) { this.exercise = exercise; }
}
```

- [ ] **Step 3 — Annoter `TrainingSession` (avec audit, version, et collection `exercises` mappée)**

```java
package com.fittracker.training;

import com.fittracker.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "training_sessions")
@EntityListeners(AuditingEntityListener.class)
public class TrainingSession {

  @Id
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;

  @Column(name = "started_at", nullable = false)
  private OffsetDateTime startedAt;

  @Column(name = "duration_seconds", nullable = false)
  private int durationSeconds;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SessionType type;

  @Column(length = 2000)
  private String notes;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Version
  private long version;

  @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @OrderBy("id.position ASC")
  private final List<SessionExercise> exercises = new ArrayList<>();

  public TrainingSession() {}

  public TrainingSession(UUID id, UUID userId, OffsetDateTime startedAt,
                         int durationSeconds, SessionType type, String notes,
                         OffsetDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.startedAt = startedAt;
    this.durationSeconds = durationSeconds;
    this.type = type;
    this.notes = notes;
    this.createdAt = createdAt;
  }

  // Garder les getters/setters existants Phase 3 + ajouter :
  public void setUserId(UUID userId) { this.userId = userId; }
  public User getUser() { return user; }
  public OffsetDateTime getUpdatedAt() { return updatedAt; }
  public long getVersion() { return version; }
}
```

**Important :** Modifier `TrainingSessionService.addExercise(...)` (Task 12) pour utiliser `session.getExercises().add(se); se.setSession(session); se.setExercise(...)` au lieu de construire un `SessionExercise` orphelin.

- [ ] **Step 4 — Compiler**

Run: `mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 5 — Commit**

```bash
git add src/main/java/com/fittracker/training/TrainingSession.java \
        src/main/java/com/fittracker/training/SessionExercise.java \
        src/main/java/com/fittracker/training/SessionExerciseId.java
git commit -m "feat(training): JPA entities + many-to-many with attrs via SessionExercise"
```

---

## Task 9: Convertir `Program`

**Files:**
- Modify: `src/main/java/com/fittracker/training/Program.java`

- [ ] **Step 1 — Annoter `Program`**

```java
package com.fittracker.training;

import com.fittracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "programs")
@EntityListeners(AuditingEntityListener.class)
public class Program {

  @Id
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 2000)
  private String description;

  @Column(name = "target_metric", length = 120)
  private String targetMetric;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Version
  private long version;

  // Constructeurs existants + getters/setters (copier depuis Phase 3, ajouter updatedAt/version)
}
```

- [ ] **Step 2 — Commit**

```bash
git add src/main/java/com/fittracker/training/Program.java
git commit -m "feat(training): annotate Program as JPA entity"
```

---

## Task 10: Convertir `Follow` (M-N self-référençant, clé composite)

**Files:**
- Modify: `src/main/java/com/fittracker/social/Follow.java`
- Create: `src/main/java/com/fittracker/social/FollowId.java`

- [ ] **Step 1 — Créer l'identifiant composite**

```java
package com.fittracker.social;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FollowId implements Serializable {

  @Column(name = "follower_id")
  private UUID followerId;

  @Column(name = "followee_id")
  private UUID followeeId;

  public FollowId() {}

  public FollowId(UUID followerId, UUID followeeId) {
    this.followerId = Objects.requireNonNull(followerId);
    this.followeeId = Objects.requireNonNull(followeeId);
  }

  public UUID getFollowerId() { return followerId; }
  public UUID getFolloweeId() { return followeeId; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FollowId other)) return false;
    return Objects.equals(followerId, other.followerId)
        && Objects.equals(followeeId, other.followeeId);
  }

  @Override
  public int hashCode() { return Objects.hash(followerId, followeeId); }
}
```

- [ ] **Step 2 — Annoter `Follow`**

```java
package com.fittracker.social;

import com.fittracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "follows")
public class Follow {

  @EmbeddedId
  private FollowId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("followerId")
  @JoinColumn(name = "follower_id")
  private User follower;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("followeeId")
  @JoinColumn(name = "followee_id")
  private User followee;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public Follow() {}

  public Follow(UUID followerId, UUID followeeId, OffsetDateTime createdAt) {
    this.id = new FollowId(followerId, followeeId);
    this.createdAt = createdAt;
  }

  public FollowId getId() { return id; }
  public UUID getFollowerId() { return id != null ? id.getFollowerId() : null; }
  public UUID getFolloweeId() { return id != null ? id.getFolloweeId() : null; }
  public OffsetDateTime getCreatedAt() { return createdAt; }

  public User getFollower() { return follower; }
  public void setFollower(User follower) { this.follower = follower; }
  public User getFollowee() { return followee; }
  public void setFollowee(User followee) { this.followee = followee; }

  // garder le record FollowKey existant pour rétrocompat si utilisé par le repo
  public record FollowKey(UUID followerId, UUID followeeId) {}
  public FollowKey key() { return new FollowKey(getFollowerId(), getFolloweeId()); }
}
```

- [ ] **Step 3 — Commit**

```bash
git add src/main/java/com/fittracker/social/Follow.java \
        src/main/java/com/fittracker/social/FollowId.java
git commit -m "feat(social): Follow as JPA entity with composite key"
```

---

## Task 11: Convertir `Notification` (payload JSONB)

**Files:**
- Modify: `src/main/java/com/fittracker/notification/Notification.java`

- [ ] **Step 1 — Annoter avec `@JdbcTypeCode(SqlTypes.JSON)` (natif Hibernate 6)**

```java
package com.fittracker.notification;

import com.fittracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications")
public class Notification {

  @Id
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private NotificationType type;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload = new HashMap<>();

  @Column(name = "read_at")
  private OffsetDateTime readAt;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  // Constructeurs + getters/setters Phase 3 conservés ; ajouter user getter
  public User getUser() { return user; }
  public void setUserId(UUID userId) { this.userId = userId; }
}
```

- [ ] **Step 2 — Commit**

```bash
git add src/main/java/com/fittracker/notification/Notification.java
git commit -m "feat(notification): JPA entity with JSONB payload"
```

---

## Task 12: Remplacer les repositories par des interfaces `JpaRepository`

**Files:**
- Modify: `src/main/java/com/fittracker/user/UserRepository.java`
- Modify: `src/main/java/com/fittracker/user/ProfileRepository.java`
- Modify: `src/main/java/com/fittracker/training/ExerciseRepository.java`
- Modify: `src/main/java/com/fittracker/training/TrainingSessionRepository.java`
- Modify: `src/main/java/com/fittracker/training/ProgramRepository.java`
- Modify: `src/main/java/com/fittracker/social/FollowRepository.java`
- Modify: `src/main/java/com/fittracker/notification/NotificationRepository.java`
- Delete: `src/main/java/com/fittracker/common/repository/InMemoryRepository.java`
- Modify (adapt callers): `*Service.java`, `*Seed.java`

- [ ] **Step 1 — Convertir `UserRepository`**

```java
package com.fittracker.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailIgnoreCase(String email);
  boolean existsByEmailIgnoreCase(String email);
}
```

Adapter `UserService.updateProfile`/`updateUser` aux nouveaux noms si nécessaire (`findByEmail` → `findByEmailIgnoreCase`).

- [ ] **Step 2 — Convertir `ProfileRepository`**

```java
package com.fittracker.user;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {}
```

- [ ] **Step 3 — Convertir `ExerciseRepository`**

```java
package com.fittracker.training;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {}
```

- [ ] **Step 4 — Convertir `TrainingSessionRepository` (filtrable)**

```java
package com.fittracker.training;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TrainingSessionRepository
    extends JpaRepository<TrainingSession, UUID>, JpaSpecificationExecutor<TrainingSession> {

  List<TrainingSession> findByUserId(UUID userId);
  Page<TrainingSession> findByUserId(UUID userId, Pageable pageable);
  long countByUserId(UUID userId);
}
```

- [ ] **Step 5 — Convertir `ProgramRepository`**

```java
package com.fittracker.training;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, UUID> {
  List<Program> findByUserId(UUID userId);
}
```

- [ ] **Step 6 — Convertir `FollowRepository`**

```java
package com.fittracker.social;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
  List<Follow> findByIdFollowerId(UUID followerId);
  List<Follow> findByIdFolloweeId(UUID followeeId);

  @Transactional
  long deleteByIdFollowerIdAndIdFolloweeId(UUID followerId, UUID followeeId);

  @Transactional
  long deleteByIdFollowerIdOrIdFolloweeId(UUID followerId, UUID followeeId);
}
```

- [ ] **Step 7 — Convertir `NotificationRepository`**

```java
package com.fittracker.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  Slice<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
  List<Notification> findByUserId(UUID userId);
}
```

- [ ] **Step 8 — Adapter les services pour les nouvelles signatures**

Pour chaque `*Service.java`, remplacer :
- `existsById(...)` → conservé (méthode JPA standard).
- `save(...)` → conservé.
- `findByEmail` → `findByEmailIgnoreCase` dans `AuthService` et `UserService`.
- `findAll()` → conserver tel quel (méthode JPA standard).
- Pour `TrainingSessionService.addExercise`, après création du `SessionExercise`, faire `se.setSession(session); se.setExercise(exercise);` avant `session.getExercises().add(se)`, puis `sessionRepository.save(session)`.
- Ajouter `@Transactional` (Spring) sur toutes les méthodes mutables des services (`create`, `update`, `delete`, `addExercise`).

- [ ] **Step 9 — Supprimer `InMemoryRepository`**

```bash
git rm src/main/java/com/fittracker/common/repository/InMemoryRepository.java
```

- [ ] **Step 10 — Adapter `UserSeed` et `NotificationSeed`**

`UserSeed` reste utile en dev (crée le user de test). Il doit s'exécuter APRÈS Flyway. Annoter avec `@Profile("dev")` ou utiliser `@DependsOn`. Vérifier que le user n'existe pas avant insertion.

`NotificationSeed` (s'il sème des notifs pour le user de test) doit aussi tenir compte de la base réelle.

- [ ] **Step 11 — Vérifier la compilation**

Run: `mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`. Si des erreurs apparaissent, corriger les appels des services qui dépendent encore de méthodes propres à `InMemoryRepository`.

- [ ] **Step 12 — Démarrer en local**

```bash
docker compose up -d db
SPRING_PROFILES_ACTIVE=dev mvn -q -DskipTests spring-boot:run
```

Expected: démarrage propre, migration Flyway appliquée, user de test seedé.

- [ ] **Step 13 — Commit**

```bash
git add -A
git commit -m "refactor(repository): replace InMemoryRepository with JpaRepository"
```

---

## Task 13: Pipeline RGPD — export et anonymisation

**Files:**
- Create: `src/main/java/com/fittracker/support/rgpd/RgpdExportService.java`
- Create: `src/main/java/com/fittracker/support/rgpd/RgpdExportController.java`
- Create: `src/main/java/com/fittracker/support/rgpd/UserAnonymizationService.java`
- Modify: `src/main/java/com/fittracker/user/UserController.java` (ajout `DELETE /me`)

- [ ] **Step 1 — `RgpdExportService` (agrégation JSON)**

```java
package com.fittracker.support.rgpd;

import com.fittracker.notification.NotificationRepository;
import com.fittracker.social.FollowRepository;
import com.fittracker.training.ProgramRepository;
import com.fittracker.training.TrainingSessionRepository;
import com.fittracker.user.ProfileRepository;
import com.fittracker.user.UserRepository;
import com.fittracker.common.error.NotFoundException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RgpdExportService {

  private final UserRepository users;
  private final ProfileRepository profiles;
  private final TrainingSessionRepository sessions;
  private final ProgramRepository programs;
  private final FollowRepository follows;
  private final NotificationRepository notifications;

  public RgpdExportService(UserRepository users, ProfileRepository profiles,
                           TrainingSessionRepository sessions, ProgramRepository programs,
                           FollowRepository follows, NotificationRepository notifications) {
    this.users = users;
    this.profiles = profiles;
    this.sessions = sessions;
    this.programs = programs;
    this.follows = follows;
    this.notifications = notifications;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> exportFor(UUID userId) {
    var user = users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    return Map.of(
        "user", user,
        "profile", profiles.findById(userId).orElse(null),
        "trainingSessions", sessions.findByUserId(userId),
        "programs", programs.findByUserId(userId),
        "followers", follows.findByIdFolloweeId(userId),
        "following", follows.findByIdFollowerId(userId),
        "notifications", notifications.findByUserId(userId)
    );
  }
}
```

- [ ] **Step 2 — `RgpdExportController`**

```java
package com.fittracker.support.rgpd;

import com.fittracker.common.security.CurrentUserProvider;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/export")
public class RgpdExportController {

  private final RgpdExportService service;
  private final CurrentUserProvider currentUser;

  public RgpdExportController(RgpdExportService service, CurrentUserProvider currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> export() {
    return ResponseEntity.ok(service.exportFor(currentUser.getId()));
  }
}
```

- [ ] **Step 3 — `UserAnonymizationService` (pipeline DELETE /me)**

```java
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

@Service
public class UserAnonymizationService {

  public static final UUID DELETED_SENTINEL_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final UserRepository users;
  private final ProfileRepository profiles;
  private final TrainingSessionRepository sessions;
  private final FollowRepository follows;
  private final NotificationRepository notifications;

  @PersistenceContext
  private EntityManager em;

  public UserAnonymizationService(UserRepository users, ProfileRepository profiles,
                                  TrainingSessionRepository sessions,
                                  FollowRepository follows, NotificationRepository notifications) {
    this.users = users;
    this.profiles = profiles;
    this.sessions = sessions;
    this.follows = follows;
    this.notifications = notifications;
  }

  @Transactional
  public void anonymize(UUID userId) {
    // 1. Réassigner les training_sessions vers le user sentinelle (conserve agrégats)
    em.createNativeQuery(
            "UPDATE training_sessions SET user_id = :sentinel WHERE user_id = :uid")
        .setParameter("sentinel", DELETED_SENTINEL_ID)
        .setParameter("uid", userId)
        .executeUpdate();

    // 2. Supprimer les follows (in/out)
    follows.deleteByIdFollowerIdOrIdFolloweeId(userId, userId);

    // 3. Supprimer les notifications
    em.createNativeQuery("DELETE FROM notifications WHERE user_id = :uid")
        .setParameter("uid", userId)
        .executeUpdate();

    // 4. Supprimer le profil
    profiles.deleteById(userId);

    // 5. Soft-delete le user (via @SQLDelete)
    users.findById(userId).ifPresent(users::delete);
  }
}
```

- [ ] **Step 4 — Endpoint `DELETE /api/v1/users/me`**

Dans `UserController.java`, ajouter :

```java
@org.springframework.web.bind.annotation.DeleteMapping("/me")
@org.springframework.http.HttpStatus(org.springframework.http.HttpStatus.NO_CONTENT)
public org.springframework.http.ResponseEntity<Void> deleteMe() {
  anonymizationService.anonymize(currentUser.getId());
  return org.springframework.http.ResponseEntity.noContent().build();
}
```

Et injecter `UserAnonymizationService` dans le constructeur.

- [ ] **Step 5 — Test rapide à la main**

```bash
docker compose up -d db
SPRING_PROFILES_ACTIVE=dev mvn -q -DskipTests spring-boot:run &
sleep 8
curl -s http://localhost:8080/api/v1/users/me/export | jq .user.email
```

Expected: `"test@fittracker.dev"`.

Stopper l'app.

- [ ] **Step 6 — Commit**

```bash
git add -A
git commit -m "feat(rgpd): user data export and anonymization pipeline"
```

---

## Task 14: Infra de test Testcontainers

**Files:**
- Create: `src/test/java/com/fittracker/support/AbstractIntegrationTest.java`

- [ ] **Step 1 — Base abstraite**

```java
package com.fittracker.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withReuse(true);
}
```

- [ ] **Step 2 — Vérifier le démarrage du contexte de test**

Run: `mvn -q -Dtest='com.fittracker.FitTrackerApplicationTests' test`

Expected: `Tests passed: 1`. Docker doit être actif. La 1ère exécution télécharge `postgres:16-alpine` (~80 Mo).

- [ ] **Step 3 — Commit**

```bash
git add src/test/java/com/fittracker/support/AbstractIntegrationTest.java
git commit -m "test(persistence): Testcontainers Postgres base class"
```

---

## Task 15: Tests d'intégration — démontrer les 4 types de relations

**Files:**
- Create: `src/test/java/com/fittracker/persistence/OneToOneRelationIT.java`
- Create: `src/test/java/com/fittracker/persistence/OneToManyRelationIT.java`
- Create: `src/test/java/com/fittracker/persistence/ManyToManyWithAttrsIT.java`
- Create: `src/test/java/com/fittracker/persistence/ManyToManySelfRefIT.java`
- Create: `src/test/java/com/fittracker/persistence/RgpdPipelineIT.java`

Chaque test crée son propre user, exerce la relation et asserte via le repository. Exemple représentatif :

- [ ] **Step 1 — `OneToOneRelationIT`**

```java
package com.fittracker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittracker.support.AbstractIntegrationTest;
import com.fittracker.user.Profile;
import com.fittracker.user.ProfileRepository;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class OneToOneRelationIT extends AbstractIntegrationTest {

  @Autowired UserRepository users;
  @Autowired ProfileRepository profiles;

  @Test
  @Transactional
  void user_and_profile_share_primary_key_via_one_to_one() {
    var user = new User(UUID.randomUUID(), "alice-" + UUID.randomUUID() + "@fittracker.dev",
        "hash", "Alice", OffsetDateTime.now());
    users.save(user);

    var profile = new Profile(user.getId(), 170, 65.0, 60.0, "bio");
    profile.setUser(user);
    profiles.save(profile);

    var loaded = profiles.findById(user.getId()).orElseThrow();
    assertThat(loaded.getUserId()).isEqualTo(user.getId());
    assertThat(loaded.getUser().getEmail()).isEqualTo(user.getEmail());
  }
}
```

- [ ] **Step 2 — `OneToManyRelationIT` (User → TrainingSession)**

```java
package com.fittracker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittracker.support.AbstractIntegrationTest;
import com.fittracker.training.SessionType;
import com.fittracker.training.TrainingSession;
import com.fittracker.training.TrainingSessionRepository;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OneToManyRelationIT extends AbstractIntegrationTest {

  @Autowired UserRepository users;
  @Autowired TrainingSessionRepository sessions;

  @Test
  void user_owns_many_sessions() {
    var user = users.save(new User(UUID.randomUUID(), "bob-" + UUID.randomUUID() + "@fittracker.dev",
        "h", "Bob", OffsetDateTime.now()));

    sessions.save(new TrainingSession(UUID.randomUUID(), user.getId(),
        OffsetDateTime.now(), 1800, SessionType.RUNNING, null, OffsetDateTime.now()));
    sessions.save(new TrainingSession(UUID.randomUUID(), user.getId(),
        OffsetDateTime.now(), 3600, SessionType.STRENGTH, null, OffsetDateTime.now()));

    assertThat(sessions.findByUserId(user.getId())).hasSize(2);
  }
}
```

- [ ] **Step 3 — `ManyToManyWithAttrsIT` (TrainingSession ↔ Exercise via SessionExercise)**

```java
package com.fittracker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittracker.support.AbstractIntegrationTest;
import com.fittracker.training.Exercise;
import com.fittracker.training.ExerciseCategory;
import com.fittracker.training.ExerciseRepository;
import com.fittracker.training.ExerciseUnit;
import com.fittracker.training.SessionExercise;
import com.fittracker.training.SessionType;
import com.fittracker.training.TrainingSession;
import com.fittracker.training.TrainingSessionRepository;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class ManyToManyWithAttrsIT extends AbstractIntegrationTest {

  @Autowired UserRepository users;
  @Autowired TrainingSessionRepository sessions;
  @Autowired ExerciseRepository exercises;

  @Test
  @Transactional
  void session_links_exercises_with_attributes() {
    var user = users.save(new User(UUID.randomUUID(), "carol-" + UUID.randomUUID() + "@x.dev",
        "h", "Carol", OffsetDateTime.now()));
    var session = sessions.save(new TrainingSession(UUID.randomUUID(), user.getId(),
        OffsetDateTime.now(), 1200, SessionType.STRENGTH, null, OffsetDateTime.now()));

    var squat = exercises.save(new Exercise(UUID.randomUUID(), "Squat test",
        ExerciseCategory.STRENGTH, "jambes", ExerciseUnit.REPS));

    var se = new SessionExercise(session.getId(), squat.getId(), 0,
        4, 8, 80.0, null, null);
    se.setSession(session);
    se.setExercise(squat);
    session.getExercises().add(se);
    sessions.save(session);

    var reloaded = sessions.findById(session.getId()).orElseThrow();
    assertThat(reloaded.getExercises()).hasSize(1);
    assertThat(reloaded.getExercises().get(0).getReps()).isEqualTo(8);
    assertThat(reloaded.getExercises().get(0).getWeightKg()).isEqualTo(80.0);
  }
}
```

- [ ] **Step 4 — `ManyToManySelfRefIT` (User ↔ User via Follow)**

```java
package com.fittracker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittracker.social.Follow;
import com.fittracker.social.FollowRepository;
import com.fittracker.support.AbstractIntegrationTest;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ManyToManySelfRefIT extends AbstractIntegrationTest {

  @Autowired UserRepository users;
  @Autowired FollowRepository follows;

  @Test
  void user_can_follow_another_user() {
    var alice = users.save(new User(UUID.randomUUID(), "a-" + UUID.randomUUID() + "@x.dev",
        "h", "Alice", OffsetDateTime.now()));
    var bob = users.save(new User(UUID.randomUUID(), "b-" + UUID.randomUUID() + "@x.dev",
        "h", "Bob", OffsetDateTime.now()));

    var follow = new Follow(alice.getId(), bob.getId(), OffsetDateTime.now());
    follow.setFollower(alice);
    follow.setFollowee(bob);
    follows.save(follow);

    assertThat(follows.findByIdFollowerId(alice.getId())).hasSize(1);
    assertThat(follows.findByIdFolloweeId(bob.getId())).hasSize(1);
  }
}
```

- [ ] **Step 5 — `RgpdPipelineIT`**

```java
package com.fittracker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittracker.notification.Notification;
import com.fittracker.notification.NotificationRepository;
import com.fittracker.notification.NotificationType;
import com.fittracker.support.AbstractIntegrationTest;
import com.fittracker.support.rgpd.RgpdExportService;
import com.fittracker.support.rgpd.UserAnonymizationService;
import com.fittracker.training.SessionType;
import com.fittracker.training.TrainingSession;
import com.fittracker.training.TrainingSessionRepository;
import com.fittracker.user.Profile;
import com.fittracker.user.ProfileRepository;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RgpdPipelineIT extends AbstractIntegrationTest {

  @Autowired UserRepository users;
  @Autowired ProfileRepository profiles;
  @Autowired TrainingSessionRepository sessions;
  @Autowired NotificationRepository notifications;
  @Autowired RgpdExportService exportService;
  @Autowired UserAnonymizationService anonymization;

  @Test
  void delete_me_anonymizes_sessions_and_purges_personal_data() {
    var user = users.save(new User(UUID.randomUUID(), "rgpd-" + UUID.randomUUID() + "@x.dev",
        "h", "Rgpd", OffsetDateTime.now()));
    var profile = new Profile(user.getId(), 180, 80.0, 78.0, "bio");
    profile.setUser(user);
    profiles.save(profile);

    var sessionId = UUID.randomUUID();
    sessions.save(new TrainingSession(sessionId, user.getId(),
        OffsetDateTime.now(), 1800, SessionType.RUNNING, null, OffsetDateTime.now()));

    var notif = new Notification(UUID.randomUUID(), user.getId(),
        NotificationType.NEW_PR, new HashMap<>(), OffsetDateTime.now());
    notifications.save(notif);

    anonymization.anonymize(user.getId());

    assertThat(users.findById(user.getId())).isEmpty(); // soft-delete masque
    assertThat(profiles.findById(user.getId())).isEmpty();
    assertThat(notifications.findByUserId(user.getId())).isEmpty();
    // La session existe encore mais a été réassignée au sentinelle
    var sentinelSessions = sessions.findByUserId(UserAnonymizationService.DELETED_SENTINEL_ID);
    assertThat(sentinelSessions).extracting(TrainingSession::getId).contains(sessionId);
  }
}
```

- [ ] **Step 6 — Exécuter la suite IT**

Run: `mvn -q test`
Expected: tous les tests passent (Phase 3 MockMvc + nouveaux IT).

- [ ] **Step 7 — Commit**

```bash
git add src/test/java/com/fittracker/persistence/
git commit -m "test(persistence): IT covering all four relation types + RGPD pipeline"
```

---

## Task 16: Documentation — `docs/architecture.md` avec ERD

**Files:**
- Create: `docs/architecture.md`
- Modify: `README.md`

- [ ] **Step 1 — Écrire `docs/architecture.md`**

Contenu minimal exigé par le brief §5 :
1. Vue d'ensemble des containers (Nginx → app → Postgres/Redis).
2. ERD Mermaid des 8 entités.
3. Choix techniques justifiés (JPA + Flyway, soft-delete, JSONB, audit, anonymisation).

ERD Mermaid à inclure :

```mermaid
erDiagram
    USERS ||--o| PROFILES : "one-to-one"
    USERS ||--o{ TRAINING_SESSIONS : "owns"
    USERS ||--o{ PROGRAMS : "owns"
    USERS ||--o{ NOTIFICATIONS : "receives"
    USERS ||--o{ FOLLOWS : "follower"
    USERS ||--o{ FOLLOWS : "followee"
    TRAINING_SESSIONS ||--o{ SESSION_EXERCISES : "contains"
    EXERCISES ||--o{ SESSION_EXERCISES : "used in"

    USERS {
      uuid id PK
      string email UNIQUE
      timestamptz deleted_at "RGPD soft-delete"
    }
    PROFILES { uuid user_id PK_FK }
    TRAINING_SESSIONS { uuid id PK uuid user_id FK }
    SESSION_EXERCISES {
      uuid session_id PK_FK
      uuid exercise_id PK_FK
      int position PK
    }
    PROGRAMS { uuid id PK uuid user_id FK }
    FOLLOWS {
      uuid follower_id PK_FK
      uuid followee_id PK_FK
    }
    NOTIFICATIONS { uuid id PK uuid user_id FK jsonb payload }
    EXERCISES { uuid id PK }
```

- [ ] **Step 2 — Mettre à jour le README**

Ajouter une section « Persistence » : commandes Flyway (`mvn flyway:migrate` si plugin ajouté, sinon Spring Boot fait tout), schéma rapide, lien vers `docs/architecture.md`.

- [ ] **Step 3 — Commit**

```bash
git add docs/architecture.md README.md
git commit -m "docs(architecture): ERD Mermaid + persistence justification"
```

---

## Task 17: Mise à jour CI

**Files:**
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1 — Vérifier que la CI lance bien `mvn verify` (qui exécute les IT)**

Testcontainers requiert Docker. GitHub Actions runners `ubuntu-latest` ont Docker préinstallé. Vérifier dans `.github/workflows/ci.yml` que le job `build` exécute `mvn -B verify` (et non `mvn -B verify -DskipITs`, sauf si un job séparé `integration-test` est créé).

Si le workflow Phase 1 skippait les IT, retirer `-DskipITs` de la commande de build OU créer un job dédié :

```yaml
  integration-test:
    runs-on: ubuntu-latest
    needs: build
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
      - name: Run Testcontainers integration tests
        run: mvn -B test
```

- [ ] **Step 2 — Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: run Testcontainers integration tests in pipeline"
```

---

## Task 18: Vérifications finales avant ouverture de PR

- [ ] **Step 1 — Test global**

```bash
docker compose down -v
mvn -B verify
```

Expected: BUILD SUCCESS, tous les tests OK, lint OK, image Docker construite si la CI le fait localement.

- [ ] **Step 2 — Smoke test manuel des endpoints RGPD**

```bash
docker compose up -d db
SPRING_PROFILES_ACTIVE=dev mvn -q -DskipTests spring-boot:run &
sleep 8

# Export
curl -s http://localhost:8080/api/v1/users/me/export | jq 'keys'
# Expected: ["followers","following","notifications","profile","programs","trainingSessions","user"]

# Anonymisation (attention : invalide le user de test du seed dev)
# curl -X DELETE http://localhost:8080/api/v1/users/me -i
# Expected: HTTP/1.1 204
```

- [ ] **Step 3 — Vérifier le brief**

Relire `/Users/madayev/Downloads/fittracker_brief.md` §4 phase 4 et cocher mentalement chaque livrable :
- [x] Migrations Flyway V1/V2/V3
- [x] Entités JPA avec relations + audit + soft-delete + optimistic locking
- [x] Repositories JpaRepository + JpaSpecificationExecutor
- [x] CRUD + règles métier propriétaire
- [x] Endpoints RGPD `GET /me/export` + `DELETE /me`
- [x] Tests d'intégration Testcontainers
- [x] `docs/architecture.md` avec ERD

- [ ] **Step 4 — Pousser et ouvrir la PR**

```bash
git push -u origin feature/phase-4-persistence
gh pr create --base main --title "Phase 4 — JPA persistence, relations, RGPD" \
  --body "$(cat <<'EOF'
## Summary
- Migrations Flyway V1/V2/V3 (schéma initial, seed exercices, sentinelle RGPD)
- Entités JPA pour les 8 entités du domain model
- Démonstration des 4 types de relations (1-1, 1-N, M-N avec attrs, M-N self-ref)
- Soft-delete via @SQLDelete + @SQLRestriction, audit @CreatedDate/@LastModifiedDate, @Version
- Endpoints RGPD `GET /api/v1/users/me/export` et `DELETE /api/v1/users/me`
- Suite Testcontainers Postgres 16 couvrant chaque relation et la pipeline d'anonymisation
- ERD Mermaid + diagramme containers dans `docs/architecture.md`

## Test plan
- [ ] `mvn -B verify` vert en local
- [ ] CI verte sur la PR
- [ ] Smoke manuel des endpoints export et delete
EOF
)"
```

- [ ] **Step 5 — STOP : validation utilisateur**

Selon la règle projet « Après chaque phase : push → CI verte → validation utilisateur → phase suivante ». Attendre la review/merge avant d'attaquer la Phase 5.

---

## Self-review (rapide)

- **Couverture spec** : tous les bullets du brief §4 sont représentés par au moins une task. ERD, audit, soft-delete, optimistic locking, JSONB, RGPD export/delete, Testcontainers, 4 types de relations testés ✔
- **Pas de placeholders** : chaque task contient le code à appliquer (sauf répétitions de getters/setters POJO conservés).
- **Cohérence des types** : `UserAnonymizationService.DELETED_SENTINEL_ID` = UUID de V3, `findByEmailIgnoreCase` cohérent dans repo et services (à propager dans `AuthService`), `SessionExerciseId` utilisée dans Task 8 et Task 15.
- **Commits atomiques** : 1 task = 1 commit, message Conventional Commits.
- **Risque connu** : `@SoftDelete` Hibernate change le comportement de `findById` (filtre les soft-deleted). Le `RgpdPipelineIT` Step 5 vérifie déjà que `users.findById` revient vide après anonymisation, comportement attendu.

---

## Execution Handoff

Plan saved to `docs/superpowers/plans/2026-06-05-phase-4-persistence.md`.

**Deux options d'exécution :**

1. **Subagent-Driven (recommandé)** — Je dispatche un subagent frais par task, review entre les tasks. Itération rapide, isolation propre.
2. **Inline Execution** — J'exécute les tasks dans cette session, batch avec checkpoints pour review.

Quelle approche ?
