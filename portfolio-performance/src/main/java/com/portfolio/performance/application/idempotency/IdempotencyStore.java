package com.portfolio.performance.application.idempotency;

import com.portfolio.performance.api.dto.AttributionRequest;
import com.portfolio.performance.api.dto.AttributionResponse;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Stores and retrieves attribution responses for idempotent request handling.
 */
public interface IdempotencyStore {

  /**
   * Returns a previously stored response for the given request id, if any.
   *
   * @param request incoming request (used for duplicate-body detection)
   * @return cached response when the request id was seen before
   */
  Optional<AttributionResponse> findCached(AttributionRequest request);

  /**
   * Stores a response for future idempotent lookups.
   *
   * @param request original request
   * @param response computed response
   */
  void store(AttributionRequest request, AttributionResponse response);

  /**
   * Returns a cached response or computes, stores, and returns a new one.
   *
   * <p>Implementations must coordinate concurrent requests for the same {@code requestId}.
   */
  AttributionResponse findOrCompute(
      AttributionRequest request, Supplier<AttributionResponse> computation);
}
