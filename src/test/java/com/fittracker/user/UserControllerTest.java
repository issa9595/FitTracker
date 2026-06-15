package com.fittracker.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittracker.auth.JwtService;
import com.fittracker.support.TestAuth;
import com.fittracker.user.dto.ProfileUpdateRequest;
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
class UserControllerTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;

  @org.junit.jupiter.api.BeforeEach
  void setup() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
  }

  @Test
  void should_return_current_user_with_hateoas_links() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me").with(TestAuth.bearerTestUser(jwtService)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000001"))
        .andExpect(jsonPath("$._links.self.href").exists())
        .andExpect(jsonPath("$._links.profile.href").exists());
  }

  @Test
  void should_return_400_problem_when_profile_update_has_invalid_height() throws Exception {
    ProfileUpdateRequest req = new ProfileUpdateRequest(10, 75.0, 72.0, "bio");
    mockMvc
        .perform(
            put("/api/v1/users/me/profile")
                .with(TestAuth.bearerTestUser(jwtService))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[?(@.field == 'heightCm')]").exists());
  }
}
