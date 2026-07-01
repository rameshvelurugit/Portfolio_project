package com.portfolio.performance.application.idempotency;

import com.portfolio.performance.api.dto.AttributionRequest;
import com.portfolio.performance.api.dto.AttributionResponse;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-memory idempotency cache keyed by {@code requestId}.
 *
 * <p>Intended for local development and unit tests only — not safe across multiple instances.
 */
@Component
@ConditionalOnProperty(name = "portfolio.idempotency.store", havingValue = "memory")
public class InMemoryIdempotencyStore implements IdempotencyStore {

  private static final Logger log = LoggerFactory.getLogger(InMemoryIdempotencyStore.class);

  private record CacheEntry(String requestHash, AttributionResponse response) {}

  private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
  private final RequestHasher requestHasher;

  public InMemoryIdempotencyStore(RequestHasher requestHasher) {
    this.requestHasher = requestHasher;
  }

  @Override
  public Optional<AttributionResponse> findCached(AttributionRequest request) {
    CacheEntry entry = cache.get(request.requestId());
    if (entry == null) {
      return Optional.empty();
    }
    logCacheHit(request, entry.requestHash());
    return Optional.of(entry.response());
  }

  @Override
  public void store(AttributionRequest request, AttributionResponse response) {
    cache.put(request.requestId(), new CacheEntry(requestHasher.hash(request), response));
  }

  @Override
  public AttributionResponse findOrCompute(
      AttributionRequest request, Supplier<AttributionResponse> computation) {
    Optional<AttributionResponse> cached = findCached(request);
    if (cached.isPresent()) {
      return cached.get();
    }

    Object lock = locks.computeIfAbsent(request.requestId(), ignored -> new Object());
    synchronized (lock) {
      try {
        cached = findCached(request);
        if (cached.isPresent()) {
          return cached.get();
        }
        AttributionResponse response = computation.get();
        store(request, response);
        return response;
      } finally {
        locks.remove(request.requestId(), lock);
      }
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
}
