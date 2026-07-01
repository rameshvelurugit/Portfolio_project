package com.portfolio.performance.application.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.performance.api.dto.AttributionRequest;
import com.portfolio.performance.api.dto.AttributionResponse;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed idempotency cache shared across application instances.
 */
@Component
@ConditionalOnProperty(name = "portfolio.idempotency.store", havingValue = "redis", matchIfMissing = true)
public class RedisIdempotencyStore implements IdempotencyStore {

  private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyStore.class);
  private static final String PROCESSING = "PROCESSING";

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final IdempotencyProperties properties;
  private final RequestHasher requestHasher;

  public RedisIdempotencyStore(
      StringRedisTemplate redis,
      ObjectMapper objectMapper,
      IdempotencyProperties properties,
      RequestHasher requestHasher) {
    this.redis = redis;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.requestHasher = requestHasher;
  }

  @Override
  public Optional<AttributionResponse> findCached(AttributionRequest request) {
    String json = redis.opsForValue().get(responseKey(request.requestId()));
    if (json == null) {
      return Optional.empty();
    }

    IdempotencyEntry entry = readEntry(json);
    logCacheHit(request, entry.requestHash());
    return Optional.of(entry.response());
  }

  @Override
  public void store(AttributionRequest request, AttributionResponse response) {
    IdempotencyEntry entry = new IdempotencyEntry(requestHasher.hash(request), response);
    redis
        .opsForValue()
        .set(responseKey(request.requestId()), writeEntry(entry), properties.getTtl());
  }

  @Override
  public AttributionResponse findOrCompute(
      AttributionRequest request, Supplier<AttributionResponse> computation) {
    Optional<AttributionResponse> cached = findCached(request);
    if (cached.isPresent()) {
      return cached.get();
    }

    String lockKey = lockKey(request.requestId());
    Boolean acquired =
        redis.opsForValue().setIfAbsent(lockKey, PROCESSING, properties.getLockTtl());

    if (Boolean.FALSE.equals(acquired)) {
      return waitForCachedResponse(request);
    }

    try {
      cached = findCached(request);
      if (cached.isPresent()) {
        return cached.get();
      }
      AttributionResponse response = computation.get();
      store(request, response);
      return response;
    } finally {
      redis.delete(lockKey);
    }
  }

  private AttributionResponse waitForCachedResponse(AttributionRequest request) {
    long deadline = System.currentTimeMillis() + properties.getWaitTimeout().toMillis();
    while (System.currentTimeMillis() < deadline) {
      Optional<AttributionResponse> cached = findCached(request);
      if (cached.isPresent()) {
        return cached.get();
      }
      sleepBriefly();
    }
    throw new IdempotencyCoordinationException(
        "Timed out waiting for idempotent response for requestId=" + request.requestId());
  }

  private void sleepBriefly() {
    try {
      Thread.sleep(50);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IdempotencyCoordinationException("Interrupted while waiting for idempotent response");
    }
  }

  private void logCacheHit(AttributionRequest request, String cachedHash) {
    String requestHash = requestHasher.hash(request);
    if (!cachedHash.equals(requestHash)) {
      log.warn(
          "Duplicate requestId {} with different request body; returning first cached response",
          request.requestId());
    } else {
      log.info(
          "Duplicate requestId {} detected; returning cached response without recalculation",
          request.requestId());
    }
  }

  private String responseKey(String requestId) {
    return properties.getKeyPrefix() + ":" + requestId;
  }

  private String lockKey(String requestId) {
    return properties.getKeyPrefix() + ":" + requestId + ":lock";
  }

  private IdempotencyEntry readEntry(String json) {
    try {
      return objectMapper.readValue(json, IdempotencyEntry.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize idempotency cache entry", exception);
    }
  }

  private String writeEntry(IdempotencyEntry entry) {
    try {
      return objectMapper.writeValueAsString(entry);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize idempotency cache entry", exception);
    }
  }
}
