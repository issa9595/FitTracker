package com.fittracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Securite HTTP appliquee par Spring Security (Phase 6).
 *
 * <p>API stateless : pas de session, validation des JWT par le Resource Server (HS256, meme secret
 * que {@link com.fittracker.auth.JwtService}). CSRF desactive (aucun cookie de session, jeton porte
 * par l'en-tete {@code Authorization} : l'attaque CSRF n'est pas applicable, cf. docs/security.md).
 * L'authentification WebSocket reste assuree au CONNECT STOMP par {@code JwtChannelInterceptor} ;
 * {@code /ws/**} et la page de demo sont donc laisses en {@code permitAll}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, SecurityProblemHandler problemHandler)
      throws Exception {
    // .cors(withDefaults()) resout par nom le bean "corsConfigurationSource" (injection par type
    // ambigue : mvcHandlerMappingIntrospector est aussi un CorsConfigurationSource).
    http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers("/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/ws/**", "/notifications.html", "/", "/favicon.ico")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            o ->
                o.authenticationEntryPoint(problemHandler)
                    .accessDeniedHandler(problemHandler)
                    .jwt(Customizer.withDefaults()))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(problemHandler).accessDeniedHandler(problemHandler))
        .headers(
            h ->
                h.frameOptions(f -> f.deny())
                    .contentTypeOptions(Customizer.withDefaults())
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)));
    return http.build();
  }

  /** Decodeur Resource Server HS256, base sur le meme secret que JwtService (signature + expiry). */
  @Bean
  JwtDecoder jwtDecoder(@Value("${fittracker.security.jwt.secret}") String secret) {
    SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  /** Hachage des mots de passe : BCrypt cost 12 (OWASP A02). */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  /**
   * Enregistre le rate limiting uniquement sur {@code /api/v1/auth/*}, avant la chaine Spring
   * Security, pour freiner le brute-force de login meme non authentifie (OWASP A07).
   */
  @Bean
  FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(ObjectMapper objectMapper) {
    FilterRegistrationBean<RateLimitFilter> registration =
        new FilterRegistrationBean<>(new RateLimitFilter(objectMapper));
    registration.addUrlPatterns("/api/v1/auth/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  /** Whitelist CORS depuis {@code fittracker.cors.allowed-origins} (liste separee par des virgules). */
  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${fittracker.cors.allowed-origins}") List<String> allowedOrigins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setExposedHeaders(List.of("Location", "Link"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
