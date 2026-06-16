package com.fittracker.common.versioning;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fittracker.auth.JwtService;
import com.fittracker.support.TestAuth;
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
class VersioningDemoControllerTest {

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
  void should_return_v1_payload_with_deprecation_headers_when_accept_is_default() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/version-demo/widgets")
                .with(TestAuth.bearerTestUser(jwtService))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(header().string("Deprecation", "true"))
        .andExpect(header().exists("Sunset"))
        .andExpect(header().string("Link", containsString("successor-version")));
  }

  @Test
  void should_return_v2_payload_when_accept_negotiates_v2() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/version-demo/widgets")
                .with(TestAuth.bearerTestUser(jwtService))
                .accept("application/vnd.fittracker.v2+json"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/vnd.fittracker.v2+json"))
        .andExpect(jsonPath("$.version").value("v2"))
        .andExpect(jsonPath("$.count").value(2));
  }
}
