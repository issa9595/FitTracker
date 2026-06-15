package com.fittracker.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test unitaire du rate limiting (OWASP A07) : 60 requetes/minute par IP sur {@code
 * /api/v1/auth/**}, puis 429. Les autres chemins ne sont pas limites.
 */
class RateLimitFilterTest {

  private final RateLimitFilter filter = new RateLimitFilter(new ObjectMapper());

  @Test
  void should_return_429_after_60_requests_per_minute_from_same_ip() throws Exception {
    String ip = "203.0.113.7";
    for (int i = 0; i < 60; i++) {
      assertThat(callAuth(ip).getStatus()).isEqualTo(200);
    }
    MockHttpServletResponse blocked = callAuth(ip);
    assertThat(blocked.getStatus()).isEqualTo(429);
    assertThat(blocked.getContentType()).contains("application/problem+json");
  }

  @Test
  void should_isolate_buckets_per_ip() throws Exception {
    for (int i = 0; i < 60; i++) {
      callAuth("198.51.100.1");
    }
    // Une IP differente garde son propre quota.
    assertThat(callAuth("198.51.100.2").getStatus()).isEqualTo(200);
  }

  @Test
  void should_not_rate_limit_non_auth_paths() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
    request.setRemoteAddr("203.0.113.9");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    assertThat(response.getStatus()).isEqualTo(200);
  }

  private MockHttpServletResponse callAuth(String ip) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
    request.setRemoteAddr(ip);
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }
}
