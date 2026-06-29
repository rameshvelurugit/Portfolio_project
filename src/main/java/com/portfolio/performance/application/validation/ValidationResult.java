package com.portfolio.performance.application.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the outcome of business-rule validation.
 *
 * <p>Use the factory methods {@link #valid()} and {@link #invalid(String)} for clarity.
 */
public final class ValidationResult {

  private final List<String> reasons;

  private ValidationResult(List<String> reasons) {
    this.reasons = reasons;
  }

  public static ValidationResult valid() {
    return new ValidationResult(List.of());
  }

  public static ValidationResult invalid(String reason) {
    return new ValidationResult(List.of(reason));
  }

  public static ValidationResult invalid(List<String> reasons) {
    return new ValidationResult(new ArrayList<>(reasons));
  }

  public boolean isValid() {
    return reasons.isEmpty();
  }

  public List<String> reasons() {
    return Collections.unmodifiableList(reasons);
  }
}
