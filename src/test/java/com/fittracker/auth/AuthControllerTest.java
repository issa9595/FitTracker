package com.fittracker.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittracker.auth.dto.LoginRequest;
import com.fittracker.auth.dto.RegisterRequest;
import com.fittracker.user.UserSeed;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private ObjectMapper objectMapper;

  private MockMvc mockMvc;

  @org.junit.jupiter.api.BeforeEach
  void setup() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
  }

  @Test
  void should_register_user_when_request_is_valid() throws Exception {
    String email = "newuser-" + System.nanoTime() + "@example.com";
    RegisterRequest body = new RegisterRequest(email, "ChangeMe123!", "New User");
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.tokenType").value("bearer"));
  }

  @Test
  void should_return_400_problem_when_email_is_invalid() throws Exception {
    RegisterRequest body = new RegisterRequest("not-an-email", "ChangeMe123!", "User");
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("https://fittracker.dev/problems/validation"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists());
  }

  @Test
  void should_login_and_return_jwt_when_credentials_match() throws Exception {
    // Utilise le user de demo seede (UserSeed) avec son mot de passe BCrypt.
    LoginRequest body = new LoginRequest("test@fittracker.dev", UserSeed.TEST_USER_PASSWORD);
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@fittracker.dev"))
        .andExpect(jsonPath("$.tokenType").value("bearer"))
        // Un access token JWT a trois segments separes par des points.
        .andExpect(jsonPath("$.accessToken").value(org.hamcrest.Matchers.matchesPattern("[^.]+\\.[^.]+\\.[^.]+")));
  }

  @Test
  void should_return_401_problem_when_password_is_wrong() throws Exception {
    LoginRequest body = new LoginRequest("test@fittracker.dev", "WrongPassword999!");
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.type").value("https://fittracker.dev/problems/unauthorized"));
  }
}
