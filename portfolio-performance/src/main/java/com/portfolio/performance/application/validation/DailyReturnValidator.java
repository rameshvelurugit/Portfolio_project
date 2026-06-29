package com.portfolio.performance.application.validation;

import com.portfolio.performance.api.dto.DailyReturnRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates business rules that cannot be expressed with Bean Validation alone.
 */
@Component
public class DailyReturnValidator {

  private static final BigDecimal ZERO = BigDecimal.ZERO;

  /**
   * Checks all business validation rules for the given request.
   *
   * @param request incoming daily return request
   * @return a {@link ValidationResult} containing any rejection reasons
   */
  public ValidationResult validate(DailyReturnRequest request) {
    List<String> reasons = new ArrayList<>();

    if (isNegative(request.beginMarketValue())) {
      reasons.add("beginMarketValue must be non-negative");
    }

    if (isNegative(request.endMarketValue())) {
      reasons.add("endMarketValue must be non-negative");
    }

    if (!StringUtils.hasText(request.currency())) {
      reasons.add("currency is required");
    }

    if (isZeroBeginWithNonZeroEnd(request)) {
      reasons.add(
          "cannot compute return when beginMarketValue is zero and endMarketValue is non-zero");
    }

    if (reasons.isEmpty()) {
      return ValidationResult.valid();
    }
    return ValidationResult.invalid(reasons);
  }

  private boolean isNegative(BigDecimal value) {
    return value.compareTo(ZERO) < 0;
  }

  private boolean isZeroBeginWithNonZeroEnd(DailyReturnRequest request) {
    return request.beginMarketValue().compareTo(ZERO) == 0
        && request.endMarketValue().compareTo(ZERO) != 0;
  }
}
