package com.fittracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Value("${spring.application.name:fittracker}")
  private String appName;

  @Value("${APP_VERSION:0.1.0-SNAPSHOT}")
  private String appVersion;

  @Bean
  public OpenAPI fitTrackerOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("FitTracker API")
                .description(
                    "API REST de suivi d'entrainement sportif. Conventions : pluriel, HATEOAS,"
                        + " erreurs RFC 7807 (application/problem+json), pagination offset/cursor,"
                        + " filtrage DSL.")
                .version(appVersion)
                .contact(new Contact().name("FitTracker").url("https://github.com/issa9595/FitTracker"))
                .license(new License().name("MIT")))
        .servers(
            List.of(
                new Server().url("http://localhost:8080").description("Local dev"),
                new Server().url("http://localhost").description("Via Nginx reverse-proxy")));
  }
}
