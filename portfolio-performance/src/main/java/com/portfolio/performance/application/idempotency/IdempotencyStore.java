package com.portfolio.performance.application.idempotency;

import com.portfolio.performance.api.dto.AttributionRequest;
import com.portfolio.performance.api.dto.AttributionResponse;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-memory idempotency cache keyed by {@code requestId}.
 *
 * <p>Entries are lost on application restart.
 */
@Component
public class IdempotencyStore {

  private static final Logger log = LoggerFactory.getLogger(IdempotencyStore.class);

  private record CacheEntry(AttributionRequest request, AttributionResponse response) {}

  private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

  /**
   * Returns a previously stored response for the given request id, if any.
   *
   * @param request incoming request (used for duplicate-body detection)
   * @return cached response when the request id was seen before
   */
  public Optional<AttributionResponse> findCached(AttributionRequest request) {
    CacheEntry entry = cache.get(request.requestId());
    if (entry == null) {
      return Optional.empty();
    }

    if (!entry.request().equals(request)) {
      log.warn(
          "Duplicate requestId {} with different request body; returning first cached response",
          request.requestId());
    } else {
      log.info(
          "Duplicate requestId {} detected; returning cached response without recalculation",
          request.requestId());
    }
    return Optional.of(entry.response());
  }

  /**
   * Stores a response for future idempotent lookups.
   *
   * @param request original request
   * @param response computed response
   */
  public void store(AttributionRequest request, AttributionResponse response) {
    cache.put(request.requestId(), new CacheEntry(request, response));
  }
}
