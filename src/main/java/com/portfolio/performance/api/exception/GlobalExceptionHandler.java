package com.portfolio.performance.api.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts framework and validation exceptions into consistent JSON error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception) {

    List<ApiErrorResponse.FieldError> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldError)
            .toList();

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Validation failed",
        "One or more request fields are invalid",
        fieldErrors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
      HttpMessageNotReadableException exception) {

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Malformed request",
        "Request body is missing or not valid JSON",
        List.of());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception) {

    List<ApiErrorResponse.FieldError> fieldErrors =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    new ApiErrorResponse.FieldError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Validation failed",
        "One or more request fields are invalid",
        fieldErrors);
  }

  private ApiErrorResponse.FieldError toFieldError(FieldError fieldError) {
    return new ApiErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage());
  }

  private ResponseEntity<ApiErrorResponse> buildErrorResponse(
      HttpStatus status, String error, String message, List<ApiErrorResponse.FieldError> fieldErrors) {

    ApiErrorResponse body =
        new ApiErrorResponse(Instant.now(), status.value(), error, message, fieldErrors);
    return ResponseEntity.status(status).body(body);
  }
}
