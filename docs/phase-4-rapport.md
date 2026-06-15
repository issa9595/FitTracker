# Phase 4 — Rapport de livraison (Persistance JPA / Flyway + RGPD)

- **Branche :** `feature/phase-4-persistence`
- **Commit :** `c10a80b` — `feat(persistence): JPA + Flyway persistence layer with RGPD pipeline (phase 4)`
- **Pull Request :** [#4 → main](https://github.com/issa9595/FitTracker/pull/4)
- **Date :** 2026-06-15
- **Statut :** `./mvnw verify` **vert** (18 tests unitaires + 5 IT Testcontainers). En attente CI GitHub Actions avant merge.

---

## 1. Objectif

Remplacer les dépôts in-memory des phases 1→3 par une persistance **Spring Data JPA sur PostgreSQL**, schéma géré par **Flyway** (Hibernate `ddl-auto=validate`), le tout validé par des **tests d'intégration Testcontainers**. La surface publique (API REST) des phases 1→3 reste inchangée.

---

## 2. Périmètre livré

- **8 entités JPA** couvrant les 4 types de relations :
  - `User` / `Profile` **1‑1** via `@MapsId` (clé primaire partagée)
  - `User` → Sessions / Programs / Notifications **1‑N**
  - `Session` / `Exercise` **M‑N avec attributs** via `SessionExercise` (`@EmbeddedId`)
  - `Follow` **M‑N auto‑référentielle** (`@EmbeddedId`)
- Soft‑delete RGPD (`@SQLDelete` + `@SQLRestriction`), verrouillage optimiste (`@Version`), audit automatique (`@CreatedDate` / `@LastModifiedDate`).
- Migrations **Flyway V1** (schéma), **V2** (seed exercices), **V3** (user sentinelle RGPD).
- **Pipeline RGPD** : `GET /api/v1/users/me/export` (Maps scalaires construites en transaction, aucune sérialisation d'entité) et `DELETE /api/v1/users/me` (réassignation des sessions au sentinelle, purge des données personnelles, soft‑delete du compte).
- IT Testcontainers (`jdbc:tc:postgresql:16-alpine`) démontrant chaque relation + le pipeline RGPD ; plugin **failsafe** câblé pour exécuter les `*IT`.

---

## 3. Fichiers créés / modifiés / supprimés

Récapitulatif du commit `c10a80b` : **16 ajoutés, 32 modifiés, 2 supprimés**.

### Ajoutés (16)
| Fichier | Rôle |
|---|---|
| `src/main/java/com/fittracker/social/FollowId.java` | Clé composite `@Embeddable` de `Follow` |
| `src/main/java/com/fittracker/training/SessionExerciseId.java` | Clé composite `@Embeddable` de `SessionExercise` |
| `src/main/java/com/fittracker/support/AuditingConfig.java` | `@EnableJpaAuditing` + `DateTimeProvider` OffsetDateTime |
| `src/main/java/com/fittracker/support/rgpd/RgpdExportController.java` | Endpoint `GET /users/me/export` |
| `src/main/java/com/fittracker/support/rgpd/RgpdExportService.java` | Construction de l'export (Maps scalaires) |
| `src/main/java/com/fittracker/support/rgpd/UserAnonymizationService.java` | Anonymisation `DELETE /users/me` |
| `src/main/resources/db/migration/V1__init.sql` | Schéma initial |
| `src/main/resources/db/migration/V2__seed_exercises.sql` | Seed des exercices |
| `src/main/resources/db/migration/V3__seed_deleted_user.sql` | User sentinelle RGPD (`…000`) |
| `src/test/java/com/fittracker/support/AbstractIntegrationTest.java` | Base commune des IT (profil test) |
| `src/test/java/com/fittracker/persistence/OneToOneRelationIT.java` | IT relation 1‑1 (`@MapsId`) |
| `src/test/java/com/fittracker/persistence/OneToManyRelationIT.java` | IT relation 1‑N |
| `src/test/java/com/fittracker/persistence/ManyToManyWithAttrsIT.java` | IT M‑N avec attributs |
| `src/test/java/com/fittracker/persistence/ManyToManySelfRefIT.java` | IT M‑N auto‑référentielle |
| `src/test/java/com/fittracker/persistence/RgpdPipelineIT.java` | IT export + anonymisation RGPD |
| `docs/architecture.md` | Documentation d'architecture |

### Supprimés (2)
| Fichier | Raison |
|---|---|
| `src/main/java/com/fittracker/common/repository/InMemoryRepository.java` | Remplacé par `JpaRepository` |
| `src/main/java/com/fittracker/training/ExerciseSeed.java` | Remplacé par Flyway `V2` |

> Également supprimé (hors versionnement, dossier non suivi) : `Développement back/` à la racine — clone dupliqué du projet avec son propre `.git`.

### Modifiés (32)
- **Build / config / CI :** `pom.xml` (pin `testcontainers.version=1.21.4`, plugin failsafe), `.github/workflows/ci.yml`, `README.md`, `src/main/resources/application.yml`, `src/main/resources/application-test.yml`.
- **Entités → JPA :** `user/User.java`, `user/Profile.java`, `training/TrainingSession.java`, `training/SessionExercise.java`, `training/Program.java`, `training/Exercise.java`, `notification/Notification.java`, `social/Follow.java`.
- **Dépôts → interfaces `JpaRepository` :** `User/Profile/TrainingSession/Exercise/Program/Notification/Follow Repository.java`.
- **Services / contrôleurs / seeds :** `auth/AuthService.java`, `user/UserService.java`, `user/UserController.java`, `user/UserSeed.java`, `training/TrainingSessionService.java`, `training/ExerciseController.java`, `training/ProgramService.java`, `notification/NotificationService.java`, `notification/NotificationSeed.java`, `social/FollowService.java`.
- **Tests :** `FitTrackerApplicationTests.java`, `training/TrainingSessionControllerTest.java`.

---

## 4. Corrections appliquées pour passer `./mvnw verify` au vert

Démarche itérative (lancer → lire la 1ʳᵉ erreur → corriger → recommencer).

| # | Symptôme | Cause racine | Correctif | Fichier |
|---|---|---|---|---|
| 1 | Testcontainers : « Could not find a valid Docker environment » (HTTP 400) alors que la CLI Docker fonctionne | Le `docker-java` embarqué par Testcontainers 1.20.x (défaut Boot 3.3.5) appelle l'API Docker **v1.32**, sous le plancher `MinAPIVersion 1.40` du moteur (Docker Desktop 29.x / API 1.54) → rejet 400 | Épingler `testcontainers.version=1.21.4` (docker‑java récent qui négocie l'API) | `pom.xml` |
| 2 | `Cannot convert unsupported date type LocalDateTime to OffsetDateTime` au `save` | L'audit Spring Data fournit un `LocalDateTime` que le bean‑wrapper ne sait pas convertir vers les champs audités `OffsetDateTime` | Fournir un `DateTimeProvider` retournant un `OffsetDateTime` (source = cible, aucune conversion) | `support/AuditingConfig.java` |
| 3 | `AssertionFailure: null identifier (Profile)` | PK partagée assignée → Spring Data croit l'entité existante et fait `merge` au lieu de `persist` sur une ligne `@MapsId` absente | `Profile implements Persistable<UUID>` (flag transient basculé par `@PostLoad`/`@PostPersist`) | `user/Profile.java` |
| 4 | `duplicate key value violates unique constraint "users_pkey"` dans le seed | `@Transactional` inopérant sur `@PostConstruct` → les deux `save` tournent dans des transactions séparées ; le user détaché est réinséré | Ouvrir une transaction programmatique unique (`TransactionTemplate`) et associer le profil à l'instance gérée retournée par `save()` | `user/UserSeed.java` |
| 5 | `500` sur `GET /training-sessions/{id}` (`LazyInitializationException` sur `exercises`) | OSIV désactivé : la collection LAZY est mappée en DTO après fermeture de la transaction | `Hibernate.initialize(session.getExercises())` dans les méthodes de lecture transactionnelles | `training/TrainingSessionService.java` |
| 6 | Violations de FK dans 2 tests | Tests référençant un `user_id` inexistant (les repos in‑memory n'imposaient pas la FK) | Créer le user prérequis dans le test ; aligner l'IT 1‑1 sur le pattern « instance gérée de `save()` » | `training/TrainingSessionControllerTest.java`, `persistence/OneToOneRelationIT.java` |

> Aucune signature ni comportement public des phases 1→3 modifié : seuls des ajustements d'infrastructure de persistance et de set‑up de tests.

---

## 5. Commandes de validation locale

> Prérequis : Docker Desktop démarré, JDK 21 (Temurin).

```bash
# Build complet : compilation + tests unitaires + IT Testcontainers + Spotless + Checkstyle
./mvnw verify

# Tests unitaires seuls (surefire)
./mvnw test

# Tests d'intégration seuls (failsafe, *IT)
./mvnw verify -DskipUnitTests=false

# Qualité
./mvnw spotless:check
./mvnw checkstyle:check
```

### Résultat obtenu
```
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0   (surefire)
[INFO] Tests run: 5,  Failures: 0, Errors: 0, Skipped: 0   (failsafe — Testcontainers)
[INFO] BUILD SUCCESS
```

---

## 6. Cohérence avec le brief (Phase 4)

- ✅ Les **4 types de relations** sont chacun démontrés par un IT dédié (`OneToOneRelationIT`, `OneToManyRelationIT`, `ManyToManyWithAttrsIT`, `ManyToManySelfRefIT`).
- ✅ **Export RGPD** : `RgpdExportService` construit des `Map<String, Object>` scalaires en transaction `readOnly` — pas de sérialisation d'entité JPA (piège du brief évité).
- ✅ **Anonymisation RGPD** : réassignation des `training_sessions` au user sentinelle `…000`, purge profil/notifications/follows, soft‑delete du compte ; vérifié par `RgpdPipelineIT`.
- ✅ Flyway gère le schéma, Hibernate en `ddl-auto=validate` (jamais `update`).
- ✅ Conventional Commits, DTO via mappers (pas de sérialisation d'entité), `@Transactional` sur les écritures multi‑entités, aucun secret en clair, SLF4J.

---

## 7. Restant à faire / suite

- [ ] **CI GitHub Actions verte** sur la PR #4 (Lint, Maven verify Testcontainers, Docker build). Ne pas merger avant le vert.
- [ ] Revue + validation utilisateur de la PR #4.
- [ ] Merge vers `main`.
- [ ] **Phase 5** (WebSockets) — non commencée, à démarrer après validation.

### Points à confirmer
- Deux fichiers non suivis ont été **laissés hors du commit** (artefacts internes, non livrables) : `docs/superpowers/plans/2026-06-05-phase-4-persistence.md` et `taches.md`. À inclure dans la PR si souhaité.
