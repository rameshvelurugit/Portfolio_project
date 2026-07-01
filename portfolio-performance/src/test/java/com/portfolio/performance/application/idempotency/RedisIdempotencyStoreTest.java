package com.portfolio.performance.application.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portfolio.performance.api.dto.AttributionRequest;
import com.portfolio.performance.api.dto.AttributionResponse;
import com.portfolio.performance.api.dto.GroupRequest;
import com.portfolio.performance.domain.AttributionStatus;
import com.redis.testcontainers.RedisContainer;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@EnabledIfDockerAvailable
class RedisIdempotencyStoreTest {

  @Container
  static final RedisContainer REDIS =
      new RedisContainer(DockerImageName.parse("redis:7-alpine"));

  private RedisIdempotencyStore store;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    LettuceConnectionFactory connectionFactory =
        new LettuceConnectionFactory(REDIS.getHost(), REDIS.getFirstMappedPort());
    connectionFactory.afterPropertiesSet();

    IdempotencyProperties properties = new IdempotencyProperties();
    properties.setStore("redis");
    properties.setTtl(Duration.ofHours(1));
    properties.setLockTtl(Duration.ofSeconds(30));
    properties.setWaitTimeout(Duration.ofSeconds(5));

    store =
        new RedisIdempotencyStore(
            new StringRedisTemplate(connectionFactory),
            objectMapper,
            properties,
            new RequestHasher(objectMapper));
  }

  @Test
  void storeAndFindCached_returnsStoredResponse() {
    AttributionRequest request = sampleRequest("REQ-REDIS-001");
    AttributionResponse response = sampleResponse(request);

    store.store(request, response);

    assertEquals(response, store.findCached(request).orElseThrow());
  }

  @Test
  void findOrCompute_whenDuplicateRequestId_returnsSameResponse() {
    AttributionRequest request = sampleRequest("REQ-REDIS-002");

    AttributionResponse first =
        store.findOrCompute(
            request,
            () -> sampleResponse(request, Instant.parse("2026-06-14T10:30:00Z")));
    AttributionResponse second = store.findOrCompute(request, () -> sampleResponse(request, Instant.now()));

    assertEquals(first, second);
    assertEquals(first.processedAt(), second.processedAt());
  }

  @Test
  void findCached_whenRequestBodyDiffers_returnsFirstResponse() {
    AttributionRequest firstRequest = sampleRequest("REQ-REDIS-003");
    AttributionResponse firstResponse =
        sampleResponse(firstRequest, Instant.parse("2026-06-14T10:30:00Z"));
    store.store(firstRequest, firstResponse);

    AttributionRequest conflictingRequest =
        new AttributionRequest(
            "REQ-REDIS-003",
            "PF-1001",
            LocalDate.of(2026, 6, 14),
            List.of(new GroupRequest("Equity", new BigDecimal("60"), new BigDecimal("9.9"), null)),
            "advisor01");

    AttributionResponse cached = store.findCached(conflictingRequest).orElseThrow();
    assertEquals(firstResponse, cached);
    assertNotEquals(
        new RequestHasher(objectMapper).hash(firstRequest),
        new RequestHasher(objectMapper).hash(conflictingRequest));
  }

  @Test
  void findOrCompute_afterTtlExpires_recomputesResponse() {
    IdempotencyProperties shortTtlProperties = new IdempotencyProperties();
    shortTtlProperties.setTtl(Duration.ofSeconds(1));
    shortTtlProperties.setLockTtl(Duration.ofSeconds(5));
    shortTtlProperties.setWaitTimeout(Duration.ofSeconds(5));

    LettuceConnectionFactory connectionFactory =
        new LettuceConnectionFactory(REDIS.getHost(), REDIS.getFirstMappedPort());
    connectionFactory.afterPropertiesSet();

    RedisIdempotencyStore shortTtlStore =
        new RedisIdempotencyStore(
            new StringRedisTemplate(connectionFactory),
            objectMapper,
            shortTtlProperties,
            new RequestHasher(objectMapper));

    AttributionRequest request = sampleRequest("REQ-REDIS-TTL");
    Instant firstProcessedAt = Instant.parse("2026-06-14T10:30:00Z");
    Instant secondProcessedAt = Instant.parse("2026-06-14T11:30:00Z");

    AttributionResponse first =
        shortTtlStore.findOrCompute(request, () -> sampleResponse(request, firstProcessedAt));

    sleep(Duration.ofMillis(1200));

    AttributionResponse second =
        shortTtlStore.findOrCompute(request, () -> sampleResponse(request, secondProcessedAt));

    assertEquals(firstProcessedAt, first.processedAt());
    assertEquals(secondProcessedAt, second.processedAt());
    assertTrue(second.processedAt().isAfter(first.processedAt()));
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }

  private static AttributionRequest sampleRequest(String requestId) {
    return new AttributionRequest(
        requestId,
        "PF-1001",
        LocalDate.of(2026, 6, 14),
        List.of(
            new GroupRequest("Equity", new BigDecimal("60"), new BigDecimal("2.5"), null),
            new GroupRequest("Fixed Income", new BigDecimal("30"), new BigDecimal("0.8"), null),
            new GroupRequest("Cash", new BigDecimal("10"), new BigDecimal("0.1"), null)),
        "advisor01");
  }

  private static AttributionResponse sampleResponse(AttributionRequest request) {
    return sampleResponse(request, Instant.parse("2026-06-14T10:30:00Z"));
  }

  private static AttributionResponse sampleResponse(
      AttributionRequest request, Instant processedAt) {
    return new AttributionResponse(
        request.requestId(),
        request.portfolioId(),
        request.valuationDate(),
        new BigDecimal("1.750"),
        AttributionStatus.VALID,
        false,
        List.of(),
        List.of(),
        processedAt);
  }
}
