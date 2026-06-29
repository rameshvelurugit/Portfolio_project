package com.portfolio.performance.api.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standard error body returned for HTTP 4xx responses caused by invalid requests.
 *
 * @param timestamp when the error occurred
 * @param status HTTP status code
 * @param error short error category
 * @param message summary message
 * @param fieldErrors per-field validation messages, when applicable
 */
public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    List<FieldError> fieldErrors) {

  /**
   * A single field-level validation error.
   *
   * @param field request field name
   * @param message validation message for that field
   */
  public record FieldError(String field, String message) {}
}
