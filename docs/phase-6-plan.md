# Phase 6 — Sécurité durcie (version CIBLÉE pour la deadline)

> Objectif : passer de l'auth « minimale » (phase 5) à une **sécurité réellement appliquée par
> Spring Security**, avec le maximum de valeur de notation (OWASP A01/A02/A07) pour un effort
> maîtrisé. Les briques les plus lourdes (OAuth2, TLS rotation, RS256) sont **documentées comme
> évolution** plutôt qu'implémentées si le temps manque.
>
> Branche : `feature/phase-6-security`, partant de `main` **après merge de la PR #5**.

## Périmètre MUST-HAVE (à faire)

### 1. Dépendances (`pom.xml`)
- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-resource-server` (validation JWT par Spring, même en HS256)
- `com.bucket4j:bucket4j-core` (rate limiting en mémoire ; Redis distribué = évolution documentée)

### 2. Hachage des mots de passe (BCrypt cost 12) — OWASP A02
- `@Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }` (dans `SecurityConfig`).
- `AuthService.register` : `passwordEncoder.encode(req.password())` au lieu de `"stub-hash-phase-6"`.
- `AuthService.login` : vérifier `passwordEncoder.matches(req.password(), user.getPasswordHash())`,
  sinon `throw new UnauthorizedException(...)` (créer une 401 RFC 7807 si absente ; sinon réutiliser
  un type d'erreur existant mappé en 401). **Important** : le `UserSeed` doit désormais encoder le
  mot de passe du user de test (ex. `passwordEncoder.encode("ChangeMe123!")`) pour que le login de la
  démo et des tests fonctionne.

### 3. `SecurityConfig` (stateless, JWT Resource Server HS256)
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource cors) throws Exception {
    http
      .csrf(csrf -> csrf.disable()) // API stateless, pas de cookie de session -> CSRF non applicable (documenté)
      .cors(c -> c.configurationSource(cors))
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
        .requestMatchers("/ws/**", "/notifications.html", "/", "/favicon.ico").permitAll()
        .anyRequest().authenticated())
      .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
      .headers(h -> h
        .frameOptions(f -> f.deny())
        .contentTypeOptions(Customizer.withDefaults())
        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)));
    return http.build();
  }

  // Décodeur HS256 basé sur le même secret que JwtService
  @Bean
  JwtDecoder jwtDecoder(@Value("${fittracker.security.jwt.secret}") String secret) {
    SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  private JwtAuthenticationConverter jwtAuthConverter() {
    JwtAuthenticationConverter c = new JwtAuthenticationConverter();
    // principal.getName() == sub == userId
    return c;
  }
  // + @Bean PasswordEncoder (BCrypt 12)
  // + @Bean CorsConfigurationSource : whitelist depuis fittracker.cors.allowed-origins
}
```
- **Supprimer `JwtAuthFilter` (phase 5)** : Spring Security gère désormais l'extraction/validation du
  JWT. Le `JwtService` reste pour **générer** les tokens au login (et pour le `JwtChannelInterceptor`
  WebSocket, qu'on **garde tel quel** — l'auth WS au CONNECT reste manuelle).
- `CurrentUserProvider.currentUserId()` : lire le `userId` depuis le `SecurityContext`
  (`SecurityContextHolder.getContext().getAuthentication().getName()` → `UUID.fromString`). Garder un
  fallback nul-safe qui lève une 401 si pas authentifié (ne plus retourner le user de test en prod).

### 4. CORS whitelist + headers de sécurité
- `CorsConfigurationSource` : origines depuis `fittracker.cors.allowed-origins` (déjà dans la config),
  méthodes GET/POST/PUT/PATCH/DELETE, headers Authorization/Content-Type.
- Headers : voir `SecurityConfig` ci-dessus (HSTS, frameOptions deny, contentTypeOptions, referrer).

### 5. Rate limiting Bucket4j (OWASP A07) sur `/api/v1/auth/*`
- Filtre `RateLimitFilter` (OncePerRequest) : 60 requêtes/min par IP sur `/api/v1/auth/**`.
  Bucket4j en mémoire (`ConcurrentHashMap<String, Bucket>`). Au dépassement → 429 `problem+json`.
- Documenter dans `docs/security.md` que la version distribuée multi-instances passerait par
  `bucket4j-redis` (Redis déjà dans la stack).

### 6. Tests (le gros morceau — adapter l'existant)
- **Support de test** : un util `TestAuth` qui génère un JWT via `JwtService` pour un userId donné, et
  un `RequestPostProcessor`/header helper `bearer(token)` pour MockMvc.
- **Mettre à jour les 8 tests controller** : ajouter `.header("Authorization", "Bearer " + token)` où
  le token correspond au user de test seedé (`CurrentUserProvider.TEST_USER_ID`). Sans token → 401
  désormais attendu.
- **Nouveaux tests d'intégration** (OWASP) :
  - `should_return_401_when_no_token` : GET `/api/v1/users/me` sans token → 401.
  - `should_return_403_or_404_when_accessing_other_users_resource` : user A avec son token tente
    d'accéder à une session de user B → 403 (le service vérifie déjà le propriétaire).
- Le `AuthControllerTest` (register/login) : login doit fournir le bon mot de passe BCrypt ; vérifier
  que `accessToken` est un JWT valide.

### 7. Documentation `docs/security.md` (compléter)
- Checklist OWASP Top 10 : A01 (access control + tests 403), A02 (BCrypt 12, JWT signé, TLS), A03
  (JPA paramétré + `@Valid`), A05 (headers, profils prod/dev, Swagger désactivable en prod), A07
  (rate limiting, lockout = évolution), A09 (logs JSON corrélés traceId).
- Justifier CSRF désactivé (API stateless sans cookie de session).

## Périmètre STRETCH (si le temps reste — sinon DOCUMENTER en évolution)
- **Refresh tokens** : table Flyway `V4__refresh_tokens.sql` (hash, expiry, revoked), entité+repo,
  `POST /auth/refresh` (rotation) + `POST /auth/logout` (révocation). Access 15 min / refresh 7 j.
- **OAuth2 Authorization Code** (Google ou GitHub) : `spring-boot-starter-oauth2-client`, endpoints
  `/auth/oauth2/{provider}` + callback échangeant le code contre un User + JWT FitTracker.
- **TLS Nginx** : `scripts/generate-certs.sh` (self-signed) + `scripts/rotate-certs.sh` (régénère +
  `nginx -s reload`), bloc `server { listen 443 ssl; }` + port 443 dans compose. mTLS app↔Redis
  documenté.
- **RS256** : remplacer HS256 par une paire de clés RSA (rotation de clés démontrable).

## Pièges connus
- **Tous les tests controller passent à 401 sans token** : c'est LE point de friction. Les mettre à
  jour AVANT de croire à une régression.
- `NimbusJwtDecoder` HS256 exige `macAlgorithm(MacAlgorithm.HS256)` et un secret ≥ 32 octets (déjà le
  cas). Le secret doit être IDENTIQUE à celui de `JwtService`.
- Garder `/ws/**` et `/notifications.html` en `permitAll` (l'auth WS se fait au CONNECT STOMP, pas via
  Spring Security HTTP).
- `JwtChannelInterceptor` (WebSocket) : **ne pas y toucher**, il reste l'auth du handshake.
- `CurrentUserProvider` lit maintenant le `SecurityContext` : vérifier que les seeds (`@PostConstruct`,
  hors requête HTTP) n'appellent pas `currentUserId()` sans contexte. `NotificationSeed` appelle
  `currentUser.currentUserId()` → le remplacer par `CurrentUserProvider.TEST_USER_ID` en dur dans le
  seed (pas de SecurityContext au démarrage).
- Swagger en prod : profil prod doit pouvoir désactiver l'UI (`springdoc.swagger-ui.enabled=false`).

## Définition de « terminé »
`./mvnw verify` vert (tests controller mis à jour + nouveaux tests 401/403), CI verte, OWASP doc à
jour, démo : `curl` sans token → 401, avec token → 200. Puis STOP → phase 7.
