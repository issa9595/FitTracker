# Stratégie de test & couverture

> Phase 7 — Comment FitTracker est testé, comment mesurer la couverture, et ce qui est
> exclu du calcul (et pourquoi).

## 1. Pyramide de tests

Les tests sont répartis sur trois niveaux, du plus rapide/isolé au plus intégré :

| Niveau | Outils | Portée | Exemples | Phase Maven |
|---|---|---|---|---|
| **Unitaires** | JUnit 5 + **Mockito** + **AssertJ** | Logique métier des `*Service` en isolation (dépendances mockées) | `UserServiceTest`, `ProgramServiceTest`, `TrainingSessionServiceTest`, `FollowServiceTest`, `RgpdExportServiceTest`, `RateLimitFilterTest` | `test` (Surefire, `*Test`) |
| **Intégration** | **Testcontainers** (PostgreSQL 16) + Spring Boot | Mapping ORM, les 4 types de relations, pipeline RGPD, contre une vraie base | `OneToOneRelationIT`, `OneToManyRelationIT`, `ManyToManyWithAttrsIT`, `ManyToManySelfRefIT`, `RgpdPipelineIT` | `integration-test` / `verify` (Failsafe, `*IT`) |
| **Bout-en-bout WebSocket** | `WebSocketStompClient` + Testcontainers | Handshake STOMP authentifié JWT au CONNECT, réception temps réel sur `/topic/...` | `NotificationWebSocketIT` | `verify` (Failsafe) |

Les tests de contrôleurs (`*ControllerTest`, `@SpringBootTest` + `MockMvc` avec
`springSecurity()`) exercent la chaîne HTTP complète (sécurité, sérialisation, HATEOAS,
RFC 7807) contre le contexte Spring réel.

## 2. Convention de nommage

Toutes les méthodes de test suivent le format **`should_<expected>_when_<context>`** (les
underscores sont explicitement autorisés par la règle Checkstyle `MethodName`). Exemples :

```java
should_create_follow_when_valid()
should_throw_forbidden_when_session_not_owned()
should_return_429_after_60_requests_per_minute_from_same_ip()
```

## 3. Commandes

```bash
# Tests unitaires seuls (rapide, Surefire) — pas de Docker requis
./mvnw test

# Tout : unitaires + intégration Testcontainers + fusion couverture + règle de couverture
# (Docker doit tourner pour Testcontainers)
./mvnw verify
```

Après `./mvnw verify`, le rapport de couverture est généré dans :

```
target/site/jacoco/index.html      # rapport HTML navigable (ouvrir dans un navigateur)
target/site/jacoco/jacoco.csv      # données par classe (parsing/scripts)
target/site/jacoco/jacoco.xml      # format machine (outils tiers)
```

En CI, ce rapport est publié en artefact **`jacoco-report-java-21`** (job `build`,
téléchargeable depuis l'onglet *Actions* → run → *Artifacts*), même si la règle de
couverture échoue, pour faciliter le diagnostic.

## 4. Mesure de la couverture — double agent JaCoCo

Les tests étant répartis entre Surefire (`*Test`) et Failsafe (`*IT`), la couverture est
collectée par **deux agents JaCoCo** distincts puis fusionnée :

| Étape JaCoCo | Phase | Fichier produit |
|---|---|---|
| `prepare-agent` | `initialize` | `target/jacoco.exec` (couverture des tests unitaires) |
| `prepare-agent-integration` | `pre-integration-test` | `target/jacoco-it.exec` (couverture des `*IT`) |
| `merge` | `verify` | `target/jacoco-merged.exec` (fusion des deux) |
| `report` | `verify` | `target/site/jacoco/*` (rapport sur la fusion) |
| `check` | `verify` | applique la règle de couverture sur la fusion |

> ⚠️ **Piège** : sans `prepare-agent-integration` + `merge`, la couverture obtenue par les
> tests d'intégration (`*IT`, Failsafe) serait ignorée et le ratio chuterait artificiellement.
> Le rapport et la règle `check` lisent tous deux `jacoco-merged.exec`.

## 5. Couverture par couche et exclusions

Cible du brief : **≥ 80 % sur les `*Service`**, **≥ 70 % global**. Taux réels mesurés :

| Couche | Lignes couvertes | Taux |
|---|---|---|
| **Services** (`*Service`, agrégé) | 329 / 334 | **98,5 %** |
| Service le plus bas (`JwtService`) | 24 / 27 | 88,9 % |
| **Global** (hors exclusions) | 918 / 1159 | **79,2 %** |

### Règle de couverture (`jacoco:check`)

Deux règles, calibrées au-dessus de la cible du brief avec une marge confortable :

1. **`element=CLASS`, `includes=*Service`** → chaque service couvre **≥ 80 %** de ses lignes
   (le plus bas mesuré est à 88,9 %).
2. **`element=BUNDLE`** → couverture **globale ≥ 70 %** des lignes.

Une couverture sous le seuil **fait échouer `./mvnw verify`** (et donc la CI).

### Ce qui est exclu du dénominateur (et pourquoi)

| Exclusion | Raison |
|---|---|
| `**/dto/**` | Records de transport sans logique (uniquement des composants générés/validés). |
| `**/config/**` | Configuration d'infrastructure (sécurité, WebSocket, OpenAPI) testée via les tests d'intégration, pas unitairement. |
| `**/*Application.*` | Point d'entrée Spring Boot (méthode `main`), non significatif. |

Les entités JPA (getters/setters), repositories (interfaces Spring Data) et mappers MapStruct
restent **dans** le dénominateur : ils sont largement couverts par les tests d'intégration et
les tests unitaires de services, et le global atteint 79,2 % en les comptant.

## 6. Cucumber (BDD)

Optionnel au brief, **non implémenté** dans cette version allégée (priorité donnée à la
couverture mesurée et au pipeline de release). La couche d'acceptation est assurée par les
`*ControllerTest` (chaîne HTTP complète) et les `*IT` (base réelle).
