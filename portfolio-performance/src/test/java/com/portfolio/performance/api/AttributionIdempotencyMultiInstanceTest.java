package com.portfolio.performance.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.performance.PortfolioPerformanceApplication;
import com.redis.testcontainers.RedisContainer;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies attribution idempotency across multiple application instances sharing one Redis cache.
 */
@Testcontainers
@EnabledIfDockerAvailable
class AttributionIdempotencyMultiInstanceTest {

  @Container
  static final RedisContainer REDIS =
      new RedisContainer(DockerImageName.parse("redis:7-alpine"));

  private static ConfigurableApplicationContext context1;
  private static ConfigurableApplicationContext context2;
  private static RestClient client1;
  private static RestClient client2;
  private static ObjectMapper objectMapper;

  @BeforeAll
  static void startTwoInstances() {
    objectMapper = new ObjectMapper().findAndRegisterModules();

    int port1 = allocatePort();
    int port2 = allocatePort();

    context1 = startInstance(port1);
    context2 = startInstance(port2);

    client1 = RestClient.builder().baseUrl("http://localhost:" + port1).build();
    client2 = RestClient.builder().baseUrl("http://localhost:" + port2).build();
  }

  @AfterAll
  static void stopInstances() {
    if (context1 != null) {
      context1.close();
    }
    if (context2 != null) {
      context2.close();
    }
  }

  @Test
  void duplicateRequestId_acrossInstances_returnsIdenticalResponse() throws Exception {
    String body = attributionPayload("REQ-MULTI-001", "2.5");

    JsonNode first = postAttribution(client1, body);
    JsonNode second = postAttribution(client2, body);

    assertThat(second.get("processedAt").asText()).isEqualTo(first.get("processedAt").asText());
    assertThat(second.get("totalContributionPct").asText())
        .isEqualTo(first.get("totalContributionPct").asText());
    assertThat(second.get("status").asText()).isEqualTo("VALID");
  }

  @Test
  void sameRequestId_differentBody_secondInstance_returnsFirstResponse() throws Exception {
    JsonNode first = postAttribution(client1, attributionPayload("REQ-MULTI-CONFLICT", "2.5"));
    JsonNode second = postAttribution(client2, attributionPayload("REQ-MULTI-CONFLICT", "9.9"));

    assertThat(second.get("processedAt").asText()).isEqualTo(first.get("processedAt").asText());
    assertThat(second.get("groups").get(0).get("effectiveReturnPct").asText()).isEqualTo("2.5");
  }

  @Test
  void invalidInput_isCachedAcrossInstances() throws Exception {
    String body =
        """
        {
          "requestId": "REQ-MULTI-INVALID",
          "portfolioId": "PF-1001",
          "valuationDate": "2026-06-14",
          "requestedBy": "advisor01",
          "groups": [
            { "groupName": "Equity", "weightPct": 50, "returnPct": 2.5 },
            { "groupName": "Fixed Income", "weightPct": 30, "returnPct": 0.8 }
          ]
        }
        """;

    JsonNode first = postAttribution(client1, body);
    JsonNode second = postAttribution(client2, body);

    assertThat(first.get("status").asText()).isEqualTo("INVALID_INPUT");
    assertThat(second.get("processedAt").asText()).isEqualTo(first.get("processedAt").asText());
    assertThat(second.get("status").asText()).isEqualTo("INVALID_INPUT");
  }

  private static ConfigurableApplicationContext startInstance(int port) {
    return new SpringApplicationBuilder(PortfolioPerformanceApplication.class)
        .bannerMode(Banner.Mode.OFF)
        .run(
            "--spring.profiles.active=it",
            "--server.port=" + port,
            "--spring.data.redis.host=" + REDIS.getHost(),
            "--spring.data.redis.port=" + REDIS.getFirstMappedPort(),
            "--portfolio.idempotency.store=redis",
            "--management.tracing.sampling.probability=0.0",
            "--spring.jmx.enabled=false",
            "--spring.main.lazy-initialization=true");
  }

  private static int allocatePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to allocate a free port", exception);
    }
  }

  private static JsonNode postAttribution(RestClient client, String body) throws Exception {
    String response =
        client
            .post()
            .uri("/api/performance/attribution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(String.class);

    return objectMapper.readTree(response);
  }

  private static String attributionPayload(String requestId, String equityReturnPct) {
    return """
        {
          "requestId": "%s",
          "portfolioId": "PF-1001",
          "valuationDate": "2026-06-14",
          "requestedBy": "advisor01",
          "groups": [
            { "groupName": "Equity", "weightPct": 60, "returnPct": %s },
            { "groupName": "Fixed Income", "weightPct": 30, "returnPct": 0.8 },
            { "groupName": "Cash", "weightPct": 10, "returnPct": 0.1 }
          ]
        }
        """
        .formatted(requestId, equityReturnPct);
  }
}
