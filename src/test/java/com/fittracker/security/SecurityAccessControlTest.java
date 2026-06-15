package com.fittracker.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fittracker.auth.JwtService;
import com.fittracker.common.security.CurrentUserProvider;
import com.fittracker.support.TestAuth;
import com.fittracker.training.SessionType;
import com.fittracker.training.TrainingSession;
import com.fittracker.training.TrainingSessionRepository;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tests d'access control OWASP A01 : la securite est reellement appliquee par Spring Security.
 *
 * <ul>
 *   <li>Sans jeton, une ressource protegee renvoie 401 (et non l'ancien utilisateur de repli).
 *   <li>Avec un jeton valide, un utilisateur ne peut pas acceder a la ressource d'un autre (403).
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityAccessControlTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private JwtService jwtService;
  @Autowired private UserRepository userRepository;
  @Autowired private TrainingSessionRepository sessionRepository;

  private MockMvc mockMvc;

  @org.junit.jupiter.api.BeforeEach
  void setup() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
  }

  @Test
  void should_return_401_when_no_token() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.type").value("https://fittracker.dev/problems/unauthorized"));
  }

  @Test
  void should_return_403_when_accessing_other_users_resource() throws Exception {
    // Une session appartenant a un autre utilisateur (user B).
    UUID otherUserId = UUID.randomUUID();
    userRepository.save(
        new User(otherUserId, "owner-" + otherUserId + "@x.dev", "h", "Owner", OffsetDateTime.now()));
    UUID sessionId = UUID.randomUUID();
    sessionRepository.save(
        new TrainingSession(
            sessionId,
            otherUserId,
            OffsetDateTime.now(),
            1800,
            SessionType.RUNNING,
            "prive",
            OffsetDateTime.now()));

    // L'utilisateur de demo (A), authentifie, tente d'y acceder -> 403.
    mockMvc
        .perform(
            get("/api/v1/training-sessions/" + sessionId)
                .with(TestAuth.bearer(jwtService, CurrentUserProvider.TEST_USER_ID)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.type").value("https://fittracker.dev/problems/forbidden"));
  }
}
