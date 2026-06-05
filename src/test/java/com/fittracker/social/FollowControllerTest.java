package com.fittracker.social;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittracker.common.security.CurrentUserProvider;
import com.fittracker.social.dto.FollowRequest;
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
class FollowControllerTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;

  private MockMvc mockMvc;

  @org.junit.jupiter.api.BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void should_follow_another_user() throws Exception {
    UUID other = UUID.randomUUID();
    userRepository.save(new User(other, "other-" + other + "@x.com", "h", "Other", OffsetDateTime.now()));

    FollowRequest req = new FollowRequest(other);
    mockMvc
        .perform(
            post("/api/v1/users/" + CurrentUserProvider.TEST_USER_ID + "/follows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.followerId").value(CurrentUserProvider.TEST_USER_ID.toString()))
        .andExpect(jsonPath("$.followeeId").value(other.toString()));
  }

  @Test
  void should_return_422_problem_when_following_oneself() throws Exception {
    FollowRequest req = new FollowRequest(CurrentUserProvider.TEST_USER_ID);
    mockMvc
        .perform(
            post("/api/v1/users/" + CurrentUserProvider.TEST_USER_ID + "/follows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.detail").value("Impossible de se suivre soi-meme"));
  }
}
