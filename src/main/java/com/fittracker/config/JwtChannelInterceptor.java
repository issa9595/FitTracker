package com.fittracker.config;

import com.fittracker.auth.JwtService;
import com.fittracker.common.security.StompPrincipal;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Authentifie la connexion WebSocket au frame STOMP CONNECT : lit le header {@code Authorization:
 * Bearer <jwt>}, valide le token et attache un {@link StompPrincipal} a la session. Une connexion
 * sans token valide est rejetee.
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

  private final JwtService jwtService;

  public JwtChannelInterceptor(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      String authorization = accessor.getFirstNativeHeader("Authorization");
      UUID userId = extractUserId(authorization);
      accessor.setUser(new StompPrincipal(userId.toString()));
    }
    return message;
  }

  private UUID extractUserId(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new IllegalArgumentException("CONNECT sans header Authorization Bearer");
    }
    String token = authorization.substring("Bearer ".length());
    return jwtService
        .validateAndExtractUserId(token)
        .orElseThrow(() -> new IllegalArgumentException("JWT invalide au CONNECT WebSocket"));
  }
}
