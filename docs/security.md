# Sécurité FitTracker — vue d'ensemble OWASP & RGPD

Ouvert dès la Phase 2 (squelette), enrichi en Phase 4 (RGPD) puis **appliqué par Spring Security en Phase 6**. Les mitigations marquées ✅ sont implémentées et couvertes par des tests ; celles marquées 🟡 sont documentées comme **évolution** (stretch) — voir la section [Évolutions documentées](#évolutions-documentées-stretch).

---

## OWASP Top 10 — état

| ID | Risque | Mitigation FitTracker | État |
|---|---|---|---|
| A01 | Broken Access Control | Spring Security (`SecurityConfig`) : tout `/api/v1/**` exige un JWT (`anyRequest().authenticated()`), vérification du propriétaire par ressource côté service (403), tests d'intégration cross-user (401 sans token, 403 sur la ressource d'autrui) | ✅ Phase 6 |
| A02 | Cryptographic Failures | Mots de passe **BCrypt cost 12** (`PasswordEncoder`), JWT signé **HS256** (secret ≥ 32 octets via `JWT_SECRET`), validation de signature/expiry par le Resource Server. TLS via Nginx (HSTS), RS256 en évolution | ✅ Phase 6 (RS256/TLS 🟡) |
| A03 | Injection | JPA paramétré (jamais de concaténation SQL), validation `@Valid` sur tous les DTO entrants, échappement HTML par défaut | ✅ Phase 3-4 |
| A04 | Insecure Design | Modélisation domaine via DDD léger, identifiants UUID non séquentiels | ✅ Phase 4 |
| A05 | Security Misconfiguration | Headers de sécurité Spring (HSTS, `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`) **et** Nginx ; CORS en whitelist ; profils `dev`/`prod` ; actuator restreint (`health,info`) ; Swagger désactivable en prod (`SWAGGER_UI_ENABLED=false`) | ✅ Phase 2/6 |
| A06 | Vulnerable Components | Dependabot ou Renovate (à activer), `mvn versions:display-dependency-updates` dans la CI mensuelle | 🟡 Phase 7 |
| A07 | Authentication Failures | **Rate limiting Bucket4j** 60 req/min/IP sur `/api/v1/auth/**` (429 `problem+json`) ; message de login générique (anti-énumération de comptes). Lockout après N échecs et MFA en évolution | ✅ Phase 6 (lockout/MFA 🟡) |
| A08 | Software & Data Integrity | Image Docker + SHA pin sur les images de base, `--platform linux/amd64` explicite dans la CI | 🟡 Phase 7 |
| A09 | Logging Failures | Logs JSON structurés avec `traceId` (Micrometer Tracing) ; erreurs RFC 7807 corrélées | ✅ Phase 2/6 |
| A10 | SSRF | Pas d'appel HTTP sortant déclenché par l'utilisateur en l'état ; si OAuth2 externe (évolution), validation stricte des URLs de redirect | 🟡 |

### Headers de sécurité actifs en Phase 2

Définis dans [`docker/nginx/conf.d/fittracker.conf`](../docker/nginx/conf.d/fittracker.conf), s'appliquent à **toutes** les réponses :

- `Strict-Transport-Security: max-age=31536000; includeSubDomains` — actif dès l'ouverture du :443 (phase 6)
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `X-XSS-Protection: 0` (désactivé conformément aux recommandations OWASP 2024)
- `Content-Security-Policy` : `default-src 'self'`, `connect-src 'self' ws: wss:` pour autoriser les WebSockets phase 5, `frame-ancestors 'none'`, `base-uri 'self'`, `form-action 'self'`

### CSRF — choix documenté

CSRF est **désactivé** dans `SecurityConfig` (`csrf.disable()`). Justification : l'API est **stateless** (`SessionCreationPolicy.STATELESS`, aucun cookie de session), l'authentification repose sur un jeton porté par l'en-tête `Authorization: Bearer`. Une attaque CSRF exploite l'envoi automatique de cookies par le navigateur ; sans cookie d'authentification, le vecteur n'est pas applicable. C'est la recommandation OWASP pour les API REST à jetons.

---

## RGPD

| Exigence | Application FitTracker | Phase |
|---|---|---|
| Minimisation | Profile contient le strict nécessaire (height/weight/goal/bio), pas de date de naissance ni localisation | 4 |
| Droit à la portabilité | `GET /api/v1/users/me/export` → JSON exhaustif des données utilisateur | 4 |
| Droit à l'effacement | `DELETE /api/v1/users/me` → pipeline d'anonymisation : `user_id` remplacé par utilisateur "deleted" sur les sessions (préserve les agrégats stats), suppression cascade des `follows`, `profile`, `notifications` | 4 |
| Soft-delete | Colonne `deleted_at` sur `users` (RGPD) via `@SQLDelete`/`@Where` Hibernate | 4 |
| Limitation de conservation | Refresh tokens : TTL 7 jours en BDD + GC automatique. Logs applicatifs : 7 jours d'historique si rotation fichier active | 2 ✅ / 6 |
| Sécurité du stockage | Mots de passe BCrypt(12), JWT RS256, TLS partout pour les flux | 6 |
| Journalisation des accès | Logs JSON avec `traceId` corrélant requête + actions BDD ; événements d'auth dédiés en phase 6 | 2 ✅ |
| Notification de violation | Procédure interne à documenter (non-applicable à un projet école, mais mentionner) | — |

---

## Chiffrement & secrets

- Secrets **jamais** versionnés. `.env` ignoré par git, `.env.example` documenté.
- En production cible :
  - Mots de passe DB injectés via mécanisme de secrets (Docker secrets, Vault, AWS Secrets Manager…).
  - Clés RSA JWT montées en read-only via `secrets:` dans docker compose (phase 6).
  - Certificats TLS rotatifs (`scripts/rotate-certs.sh` en phase 6).
- mTLS entre app et Redis ou broker mentionné comme évolution, exemple de config documentée même si non activée.

---

## Authentification & autorisation (phase 6 — implémenté)

Implémenté et testé (`./mvnw verify`) :

- **Spring Security en Resource Server** (`spring-boot-starter-oauth2-resource-server`) : validation des JWT par un `NimbusJwtDecoder` HS256 (`SecurityConfig#jwtDecoder`), même secret que `JwtService` (`fittracker.security.jwt.secret`, ≥ 32 octets, injecté via `JWT_SECRET`). L'algorithme est épinglé `MacAlgorithm.HS256` des deux côtés.
- **Stateless** : `SessionCreationPolicy.STATELESS`, aucune session HTTP. Le principal de l'`Authentication` est le `sub` du token (= userId), lu par `CurrentUserProvider#currentUserId()`.
- **Autorisation** : `/api/v1/auth/**`, `/actuator/health|info`, Swagger et `/ws/**` en `permitAll` ; tout le reste exige un JWT (`anyRequest().authenticated()`). La propriété par ressource est vérifiée côté service (403 si la ressource appartient à un autre utilisateur).
- **Mots de passe** : `BCryptPasswordEncoder(12)`. `AuthService.register` encode ; `AuthService.login` vérifie via `matches()` et renvoie une 401 générique (anti-énumération).
- **Erreurs RFC 7807** : 401/403 niveau Spring Security uniformisés en `application/problem+json` par `SecurityProblemHandler`.
- **Rate limiting** : `RateLimitFilter` (Bucket4j en mémoire), 60 req/min/IP sur `/api/v1/auth/**`, 429 `problem+json` au dépassement. Enregistré avant la chaîne Security (anti brute-force login).
- **CORS** : whitelist d'origines depuis `fittracker.cors.allowed-origins` (`SecurityConfig#corsConfigurationSource`).
- **WebSocket** : l'authentification reste assurée au CONNECT STOMP par `JwtChannelInterceptor` (phase 5) ; `/ws/**` est en `permitAll` côté HTTP.

### Authentification WebSocket

Le handshake HTTP `/ws` est `permitAll` ; le jeton est transmis dans le frame STOMP `CONNECT` (header `Authorization`) et validé par `JwtChannelInterceptor`, qui attache un `StompPrincipal`. Une connexion sans jeton valide est rejetée.

---

## Évolutions documentées (stretch)

Non implémentées dans cette phase ciblée (priorisée pour la deadline), prêtes à être ajoutées :

- **Refresh tokens révocables** : table Flyway `refresh_tokens` (hash, expiry, revoked), `POST /auth/refresh` (rotation) + `POST /auth/logout` (révocation). Access 15 min / refresh 7 j. À ce stade, seuls des access tokens (TTL configurable, défaut 15 min) sont émis.
- **OAuth2 Authorization Code** (Google/GitHub) : `spring-boot-starter-oauth2-client`, endpoints `/auth/oauth2/{provider}` + callback échangeant le code contre un User + JWT FitTracker.
- **RS256** : remplacer HS256 par une paire de clés RSA (rotation de clés démontrable) ; le Resource Server n'aurait alors que la clé publique.
- **TLS Nginx** : `scripts/generate-certs.sh` (self-signed) + `scripts/rotate-certs.sh` (régénère + `nginx -s reload`), bloc `server { listen 443 ssl; }`. mTLS app↔Redis documenté.
- **Rate limiting distribué** : la version mono-instance (`ConcurrentHashMap`) passerait à `bucket4j-redis` (Redis déjà dans la stack) pour partager le quota entre instances.
- **Lockout / MFA** : verrouillage après N échecs (compteur Redis) et MFA TOTP.

---

## Surveillance & audit

- Logs JSON corrélés (`traceId`) → ingestion possible dans Loki, ELK, Datadog…
- Métriques Micrometer exposées via `/actuator/prometheus` (à activer en phase 7).
- Événements d'auth (login OK, failed, refresh, logout) tracés à `INFO` avec `event_type` dédié.
- Healthcheck Docker → l'orchestrateur peut redémarrer un conteneur unhealthy.

---

## Checklist par phase

- [x] **Phase 2** : Headers Nginx, profils prod/dev séparés, secrets externalisés (env vars), logs JSON, base sécurité documentée.
- [x] **Phase 4** : RGPD complet (export, anonymisation, soft-delete), audit `@CreatedDate`/`@LastModifiedDate`.
- [x] **Phase 6 (ciblée)** : Spring Security Resource Server (JWT HS256), BCrypt(12), autorisation per-resource + tests OWASP (401 sans token, 403 cross-user), CORS whitelist, headers de sécurité, rate limiting Bucket4j, Swagger désactivable en prod. **Stretch documenté** : refresh tokens, OAuth2, RS256, TLS rotation, rate limiting distribué, lockout/MFA.
- [ ] **Phase 7** : SCA via Dependabot ou Renovate, image scan (Trivy ou similaire).
