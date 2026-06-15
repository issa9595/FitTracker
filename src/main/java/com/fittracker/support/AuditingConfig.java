package com.fittracker.support;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Active le remplissage automatique des champs @CreatedDate / @LastModifiedDate. */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class AuditingConfig {

  /**
   * Les champs audites sont typees {@link OffsetDateTime} (colonnes timestamptz). Le {@code
   * CurrentDateTimeProvider} par defaut fournit un {@code LocalDateTime} que le bean-wrapper de
   * Spring Data ne sait pas convertir en {@link OffsetDateTime}. En fournissant directement un
   * {@link OffsetDateTime}, la source et la cible sont du meme type : aucune conversion n'est tentee.
   */
  @Bean
  public DateTimeProvider auditingDateTimeProvider() {
    return () -> Optional.of(OffsetDateTime.now());
  }
}
