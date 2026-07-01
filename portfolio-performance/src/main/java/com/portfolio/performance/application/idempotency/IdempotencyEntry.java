package com.portfolio.performance.application.idempotency;

import com.portfolio.performance.api.dto.AttributionResponse;

/**
 * Serialized value stored in the idempotency cache.
 *
 * @param requestHash SHA-256 hash of the canonical request JSON
 * @param response cached attribution response
 */
public record IdempotencyEntry(String requestHash, AttributionResponse response) {}
