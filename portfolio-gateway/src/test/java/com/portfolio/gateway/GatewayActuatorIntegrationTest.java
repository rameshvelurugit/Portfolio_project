package com.portfolio.gateway;

import static org.springframework.test.web.reactive.server.WebTestClient.bindToApplicationContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayActuatorIntegrationTest {

  @Autowired private ApplicationContext applicationContext;

  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = bindToApplicationContext(applicationContext).build();
  }

  @Test
  void healthEndpoint_isAvailable() {
    webTestClient
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("UP");
  }

  @Test
  void infoEndpoint_returnsGatewayMetadata() {
    webTestClient
        .get()
        .uri("/actuator/info")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.service")
        .isEqualTo("portfolio-gateway");
  }
}
