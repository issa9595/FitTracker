package com.fittracker.training;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fittracker.auth.JwtService;
import com.fittracker.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ExerciseControllerTest {

  @Autowired private WebApplicationContext webApplicationContext;
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
  void should_list_strength_exercises_when_filter_by_category() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/exercises?filter=category:eq:STRENGTH&filterOp=AND&size=10")
                .with(TestAuth.bearerTestUser(jwtService)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(greaterThan(0)))
        .andExpect(jsonPath("$.content[0].category").value("STRENGTH"))
        .andExpect(jsonPath("$._links.self.href").exists());
  }

  @Test
  void should_return_400_problem_when_filter_field_is_unknown() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/exercises?filter=unknownField:eq:foo")
                .with(TestAuth.bearerTestUser(jwtService)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Requete invalide"));
  }
}
