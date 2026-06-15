package com.fittracker.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittracker.auth.JwtService;
import com.fittracker.social.Follow;
import com.fittracker.social.FollowRepository;
import com.fittracker.training.SessionType;
import com.fittracker.training.TrainingSessionService;
import com.fittracker.training.dto.TrainingSessionCreateRequest;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * Test d'integration WebSocket de bout en bout : un follower authentifie par JWT recoit en temps
 * reel une notification quand l'utilisateur qu'il suit termine une seance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationWebSocketIT {

  @LocalServerPort int port;

  @Autowired JwtService jwtService;
  @Autowired UserRepository users;
  @Autowired FollowRepository follows;
  @Autowired TrainingSessionService sessions;

  @Test
  @SuppressWarnings("unchecked")
  void follower_receives_realtime_notification_when_followee_completes_session() throws Exception {
    User followee =
        users.save(
            new User(
                UUID.randomUUID(), "b-" + UUID.randomUUID() + "@x.dev", "h", "B",
                OffsetDateTime.now()));
    User follower =
        users.save(
            new User(
                UUID.randomUUID(), "a-" + UUID.randomUUID() + "@x.dev", "h", "A",
                OffsetDateTime.now()));
    Follow follow = new Follow(follower.getId(), followee.getId(), OffsetDateTime.now());
    follow.setFollower(follower);
    follow.setFollowee(followee);
    follows.save(follow);

    String token = jwtService.generateAccessToken(follower.getId(), follower.getEmail());

    WebSocketStompClient stomp = new WebSocketStompClient(new StandardWebSocketClient());
    stomp.setMessageConverter(new MappingJackson2MessageConverter());

    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + token);

    StompSession session =
        stomp
            .connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Map<String, Object>> received = new AtomicReference<>();
    session.subscribe(
        "/topic/notifications/" + follower.getId(),
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return Map.class;
          }

          @Override
          public void handleFrame(StompHeaders headers, Object payload) {
            received.set((Map<String, Object>) payload);
            latch.countDown();
          }
        });

    // Laisser le SUBSCRIBE se propager avant de declencher l'evenement metier.
    Thread.sleep(300);

    sessions.create(
        followee.getId(),
        new TrainingSessionCreateRequest(OffsetDateTime.now(), 1800, SessionType.RUNNING, null));

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(received.get().get("type")).isEqualTo("FRIEND_SESSION_COMPLETED");

    session.disconnect();
    stomp.stop();
  }
}
