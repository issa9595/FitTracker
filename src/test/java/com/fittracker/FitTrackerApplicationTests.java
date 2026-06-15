package com.fittracker;

import com.fittracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/** Smoke test : le contexte complet demarre (JPA + Flyway + Testcontainers Postgres). */
class FitTrackerApplicationTests extends AbstractIntegrationTest {

  @Test
  void contextLoads() {}
}
