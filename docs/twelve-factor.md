# Twelve-Factor — Application FitTracker

Référence : https://12factor.net

Ce document liste les 12 facteurs et précise **où** et **comment** chacun est appliqué dans FitTracker. Mis à jour à chaque phase.

| # | Facteur | État | Référence dans le code |
|---|---|---|---|
| I | Codebase | ✅ | [`README.md`](../README.md), repo Git unique |
| II | Dependencies | ✅ | [`pom.xml`](../pom.xml), [`.mvn/`](../.mvn/) wrapper |
| III | Config | ✅ | [`application.yml`](../src/main/resources/application.yml), [`.env.example`](../.env.example) |
| IV | Backing services | ✅ | [`docker-compose.yml`](../docker-compose.yml) (db, redis, nginx) |
| V | Build, release, run | ✅ | [`Dockerfile`](../Dockerfile) multi-stage + tags `${GIT_SHA}` |
| VI | Processes | ✅ | App stateless (sessions JWT phase 6) |
| VII | Port binding | ✅ | `server.port=${SERVER_PORT:8080}` |
| VIII | Concurrency | ✅ | Thread pool Tomcat + virtual threads Java 21 |
| IX | Disposability | ✅ | Shutdown grace via Spring Boot + `restart: unless-stopped` |
| X | Dev/prod parity | ✅ | Même image Docker, profil Spring qui change |
| XI | Logs | ✅ | stdout JSON via Logback ([`logback-spring.xml`](../src/main/resources/logback-spring.xml)) |
| XII | Admin processes | 🟡 | À ajouter en phase 4 (job Flyway migration) |

---

## I — Codebase

> One codebase tracked in revision control, many deploys.

- Un seul repo Git : `github.com/issa9595/FitTracker`.
- Branche par phase, PR vers `main`, CI obligatoire avant merge.
- Une image Docker = un binaire pour tous les environnements (dev, prod). C'est le **profil Spring** qui change le comportement, pas le code.

## II — Dependencies

> Explicitly declare and isolate dependencies.

- Toutes les dépendances sont déclarées dans [`pom.xml`](../pom.xml). Aucune dépendance n'est résolue depuis l'environnement hôte.
- Le **Maven Wrapper** (`./mvnw`) fige la version de Maven (3.9.9) utilisée par tout le monde, y compris la CI.
- Aucune installation système supposée dans l'image runtime (alpine + JRE custom seulement, pas d'apt/apk runtime).

## III — Config

> Store config in the environment.

- Aucun secret en clair dans le repo. Le `.gitignore` exclut `.env`.
- [`application.yml`](../src/main/resources/application.yml) utilise systématiquement la syntaxe `${VAR:default}` :
  ```yaml
  spring:
    datasource:
      url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/fittracker}
      password: ${SPRING_DATASOURCE_PASSWORD:fittracker}
  ```
- [`application-prod.yml`](../src/main/resources/application-prod.yml) **n'a pas de défauts** pour les valeurs sensibles (DB credentials), elles doivent être fournies par l'environnement, sinon le démarrage échoue.
- [`.env.example`](../.env.example) documente l'ensemble des variables attendues, par section (App, DB, Redis, JWT…).
- `docker-compose.yml` charge `.env` via `env_file:` et exporte les variables vers chaque service.

## IV — Backing services

> Treat backing services as attached resources.

- PostgreSQL, Redis et Nginx sont des services attachés via [`docker-compose.yml`](../docker-compose.yml).
- L'app ne sait pas si `db` est sur la même machine ou un RDS distant : elle ne connaît que l'URL JDBC dans `SPRING_DATASOURCE_URL`.
- Volumes nommés (`pg-data`, `redis-data`) pour la persistance — détachables/remplaçables.

## V — Build, release, run

> Strictly separate build and run stages.

- **Build** : `./mvnw package` ou le builder du Dockerfile produit `fittracker.jar`.
- **Release** : `scripts/build-and-push.sh` tague l'image avec `${GIT_SHA}` + `latest`. L'image est immutable.
- **Run** : `docker compose up` ou `docker run` exécute l'image. Aucune compilation à l'exécution.
- Le builder Dockerfile et le runtime sont **deux stages distincts**, le runtime ne contient ni Maven ni JDK complet (juste le JRE jlink).

## VI — Processes

> Execute the app as one or more stateless processes.

- L'application est stateless : aucune session HTTP, JWT en phase 6.
- Toute donnée durable va en PostgreSQL ou Redis.
- En phase 5, les WebSockets seront stateful **par session**, mais l'état partagé entre instances ira dans Redis (broker pub/sub).

## VII — Port binding

> Export services via port binding.

- L'application **expose elle-même** son port (`server.port=${SERVER_PORT:8080}`), sans dépendre d'un serveur externe.
- Nginx vient devant comme reverse-proxy, mais l'app est self-contained et peut tourner directement.

## VIII — Concurrency

> Scale out via the process model.

- Tomcat embarqué : pool de threads de travail (Spring Boot default `server.tomcat.threads.max=200`).
- **Java 21 virtual threads** (records + virtual threads imposés par le brief stack §1) seront activés pour les workers I/O bound.
- Plusieurs instances de l'image peuvent tourner derrière Nginx (load balancing) — pas d'état local.

## IX — Disposability

> Maximize robustness with fast startup and graceful shutdown.

- Startup ~3 s grâce à JRE jlink minimal + classpath réduit.
- `restart: unless-stopped` dans `docker-compose.yml` pour chaque service.
- Spring Boot gère le shutdown gracieux (Tomcat finit les requêtes en cours, drain des bean lifecycle).
- HEALTHCHECK Docker : conteneur marqué `unhealthy` si `/actuator/health` échoue 3 fois → l'orchestrateur peut redémarrer.

## X — Dev/prod parity

> Keep development, staging, and production as similar as possible.

- **Même image Docker** déployée en dev et prod. C'est uniquement le profil Spring (`SPRING_PROFILES_ACTIVE`) qui change.
- Backing services identiques : PostgreSQL 16, Redis 7, Nginx alpine. Pas de SQLite/H2 en dev.
- `docker-compose.override.yml` ajoute en dev : exposition des ports `db:5432` et `redis:6379` sur l'hôte (debug), bind-mount du JAR pour itération rapide. La topologie reste identique.

## XI — Logs

> Treat logs as event streams.

- L'application **n'écrit pas dans des fichiers** par défaut : tout part sur stdout.
- Profil `dev` : texte coloré humainement lisible.
- Profil `prod` : **JSON structuré** via `LogstashEncoder` ([`logback-spring.xml`](../src/main/resources/logback-spring.xml)), un événement par ligne :
  ```json
  {"@timestamp":"2026-06-04T12:00:00Z","level":"INFO","logger":"com.fittracker.FitTrackerApplication","thread":"main","message":"Started FitTrackerApplication","traceId":"...","spanId":"...","app":"fittracker"}
  ```
- `traceId`/`spanId` injectés dans le MDC par **Micrometer Tracing** → corrélation transverse.
- Rotation de fichier (100 Mo, historique 7 jours) disponible mais **opt-in** via `LOG_FILE_ENABLED=true` pour les cas où l'agrégateur de logs externe n'est pas disponible. Conformité 12-Factor : la rotation est normalement la responsabilité du runtime (Docker logging driver, journald, Loki, etc.).

Validation :
```bash
docker compose logs app | jq '.message'   # parse réussit si JSON valide
```

## XII — Admin processes

> Run admin/management tasks as one-off processes.

- À mettre en place en **phase 4** avec l'arrivée de Flyway : les migrations seront exécutées par Spring Boot au démarrage, mais peuvent aussi être lancées comme processus admin :
  ```bash
  docker compose run --rm app java -jar /app/app.jar --spring.flyway.migrate-only=true
  ```
- L'export RGPD (phase 4) sera lui aussi exposable en CLI argument pour faciliter les exports batch.

---

## Validation rapide

```bash
# Lancer la stack
docker compose up -d

# Vérifier les 4 services sains
docker compose ps

# Vérifier que les logs prod sont du JSON valide
docker compose logs app --no-log-prefix | head -5 | jq .

# Vérifier que la config est bien externalisée (aucun secret committed)
git ls-files | xargs grep -l -i 'password\|secret' 2>/dev/null | grep -v -E '\.example|\.md$|application.yml'
# Doit retourner rien : tous les "password" referenced sont dans application.yml comme placeholders
```
