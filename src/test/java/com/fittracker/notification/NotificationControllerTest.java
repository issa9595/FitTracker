package com.fittracker.notification;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittracker.common.security.CurrentUserProvider;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class NotificationControllerTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private NotificationRepository repository;
  @Autowired private ObjectMapper objectMapper;

  private MockMvc mockMvc;

  @org.junit.jupiter.api.BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void should_paginate_notifications_with_cursor() throws Exception {
    // S'assure d'avoir 3+ notifs pour ce user
    for (int i = 0; i < 3; i++) {
      repository.save(
          new Notification(
              UUID.randomUUID(),
              CurrentUserProvider.TEST_USER_ID,
              NotificationType.ACHIEVEMENT,
              Map.of("seq", i),
              OffsetDateTime.now()));
    }

    var result =
        mockMvc
            .perform(get("/api/v1/notifications?size=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.nextCursor").exists())
            .andExpect(jsonPath("$._links.next.href").exists())
            .andReturn();

    String nextCursor =
        objectMapper.readTree(result.getResponse().getContentAsString()).get("nextCursor").asText();

    mockMvc
        .perform(get("/api/v1/notifications?cursor=" + nextCursor + "&size=2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(greaterThan(0)));
  }

  @Test
  void should_mark_notification_as_read() throws Exception {
    UUID id = UUID.randomUUID();
    repository.save(
        new Notification(
            id,
            CurrentUserProvider.TEST_USER_ID,
            NotificationType.NEW_PR,
            Map.of("exercise", "squat"),
            OffsetDateTime.now()));

    mockMvc
        .perform(patch("/api/v1/notifications/" + id + "/read"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.readAt").exists());
  }

  @Test
  void should_return_404_problem_when_notification_id_is_unknown() throws Exception {
    UUID unknown = UUID.fromString("11111111-1111-1111-1111-111111111111");
    mockMvc
        .perform(patch("/api/v1/notifications/" + unknown + "/read"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("https://fittracker.dev/problems/not-found"))
        .andExpect(jsonPath("$.resource").value("Notification"));
  }
}
