package com.fittracker.training;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittracker.auth.JwtService;
import com.fittracker.support.TestAuth;
import com.fittracker.training.dto.ProgramCreateRequest;
import java.time.LocalDate;
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
class ProgramControllerTest {

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
  void should_create_program_when_dates_are_consistent() throws Exception {
    ProgramCreateRequest req =
        new ProgramCreateRequest(
            "Prep semi-marathon",
            "12 semaines",
            "10km en 50min",
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 9, 15));
    mockMvc
        .perform(
            post("/api/v1/programs")
                .with(TestAuth.bearerTestUser(jwtService))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Prep semi-marathon"))
        .andExpect(jsonPath("$._links.self.href").exists());
  }

  @Test
  void should_return_422_problem_when_end_date_before_start_date() throws Exception {
    ProgramCreateRequest req =
        new ProgramCreateRequest(
            "Bad prog", null, null, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 6, 15));
    mockMvc
        .perform(
            post("/api/v1/programs")
                .with(TestAuth.bearerTestUser(jwtService))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type").value("https://fittracker.dev/problems/business-rule"))
        .andExpect(jsonPath("$.detail").value("endDate doit etre apres startDate"));
  }
}
