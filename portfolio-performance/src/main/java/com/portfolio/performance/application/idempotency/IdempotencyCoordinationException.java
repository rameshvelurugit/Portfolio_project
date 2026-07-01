package com.portfolio.performance.application.idempotency;

/**
 * Thrown when coordination across instances fails while waiting for an idempotent response.
 */
public class IdempotencyCoordinationException extends RuntimeException {

  public IdempotencyCoordinationException(String message) {
    super(message);
  }
}
