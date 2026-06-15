package com.fittracker.training;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittracker.training.dto.TrainingSessionCreateRequest;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class TrainingSessionControllerTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TrainingSessionRepository repository;
  @Autowired private UserRepository userRepository;

  private MockMvc mockMvc;

  @org.junit.jupiter.api.BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void should_create_session_then_get_it_back() throws Exception {
    TrainingSessionCreateRequest req =
        new TrainingSessionCreateRequest(OffsetDateTime.now(), 3600, SessionType.STRENGTH, "Push day");
    var result =
        mockMvc
            .perform(
                post("/api/v1/training-sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/api/v1/training-sessions/")))
            .andExpect(jsonPath("$._links.self.href").exists())
            .andReturn();
    String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

    mockMvc
        .perform(get("/api/v1/training-sessions/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.type").value("STRENGTH"));
  }

  @Test
  void should_return_403_problem_when_session_belongs_to_other_user() throws Exception {
    UUID id = UUID.randomUUID();
    UUID otherUserId = UUID.fromString("99999999-9999-9999-9999-999999999999");
    // La FK training_sessions.user_id exige un user existant : on cree le proprietaire "ennemi".
    userRepository.save(
        new User(otherUserId, "enemy-" + otherUserId + "@x.com", "h", "Enemy", OffsetDateTime.now()));
    TrainingSession foreign =
        new TrainingSession(
            id,
            otherUserId,
            OffsetDateTime.now(),
            1800,
            SessionType.RUNNING,
            "ennemi",
            OffsetDateTime.now());
    repository.save(foreign);

    mockMvc
        .perform(get("/api/v1/training-sessions/" + id))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.type").value("https://fittracker.dev/problems/forbidden"));

    mockMvc
        .perform(delete("/api/v1/training-sessions/" + id))
        .andExpect(status().isForbidden());
  }
}
