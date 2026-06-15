package com.fittracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket STOMP (Phase 5).
 *
 * <p>Broker simple en memoire ({@code /topic}) avec heartbeats 10s/10s. Endpoint d'upgrade {@code
 * /ws} (proxifie par Nginx, route {@code /ws/**}), en natif (clients Java/IT) et en SockJS
 * (navigateur). L'auth JWT au CONNECT est assuree par {@link JwtChannelInterceptor}.
 *
 * <p>Mise a l'echelle multi-instances : remplacer le SimpleBroker par un relais STOMP externe
 * (RabbitMQ) ou relayer les messages via Redis pub/sub entre instances. Voir docs/websockets.md.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtChannelInterceptor jwtChannelInterceptor;

  public WebSocketConfig(JwtChannelInterceptor jwtChannelInterceptor) {
    this.jwtChannelInterceptor = jwtChannelInterceptor;
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry
        .enableSimpleBroker("/topic")
        .setHeartbeatValue(new long[] {10000, 10000})
        .setTaskScheduler(webSocketHeartbeatScheduler());
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(jwtChannelInterceptor);
  }

  @Bean
  public TaskScheduler webSocketHeartbeatScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("ws-heartbeat-");
    scheduler.initialize();
    return scheduler;
  }
}
