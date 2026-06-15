package com.fittracker.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base des tests d'integration. Le profil "test" fournit une datasource Testcontainers via l'URL
 * "jdbc:tc:postgresql:16-alpine:///fittracker" : un conteneur Postgres reel est demarre, migre par
 * Flyway, et reutilise entre les classes de test (meme URL = meme conteneur dans la JVM). Docker
 * doit etre actif.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {}
