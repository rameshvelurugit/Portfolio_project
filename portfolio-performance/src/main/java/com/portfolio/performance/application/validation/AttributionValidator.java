package com.portfolio.performance.application.validation;

import com.portfolio.performance.api.dto.AttributionRequest;
import com.portfolio.performance.api.dto.GroupRequest;
import com.portfolio.performance.domain.CalculationConstants;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates business rules for portfolio performance attribution requests.
 */
@Component
public class AttributionValidator {

  private static final BigDecimal ZERO = BigDecimal.ZERO;

  /**
   * Checks all business validation rules for the given attribution request.
   *
   * @param request incoming attribution request
   * @return a {@link ValidationResult} containing any rejection reasons
   */
  public ValidationResult validate(AttributionRequest request) {
    List<String> reasons = new ArrayList<>();

    if (request.groups() == null || request.groups().isEmpty()) {
      reasons.add("groups must contain at least one asset group");
    } else {
      validateGroups(request.groups(), reasons);
      validateTotalWeight(request.groups(), reasons);
    }

    if (reasons.isEmpty()) {
      return ValidationResult.valid();
    }
    return ValidationResult.invalid(reasons);
  }

  private void validateGroups(List<GroupRequest> groups, List<String> reasons) {
    for (GroupRequest group : groups) {
      if (!StringUtils.hasText(group.groupName())) {
        reasons.add("groupName is required for every asset group");
      }
      if (group.weightPct() != null && group.weightPct().compareTo(ZERO) < 0) {
        reasons.add("weightPct must be non-negative for group: " + group.groupName());
      }
    }
  }

  private void validateTotalWeight(List<GroupRequest> groups, List<String> reasons) {
    BigDecimal totalWeight =
        groups.stream()
            .map(GroupRequest::weightPct)
            .filter(weight -> weight != null)
            .reduce(ZERO, BigDecimal::add);

    if (totalWeight.compareTo(CalculationConstants.MIN_TOTAL_WEIGHT_PCT) < 0
        || totalWeight.compareTo(CalculationConstants.MAX_TOTAL_WEIGHT_PCT) > 0) {
      reasons.add(
          "total weight must be between 99% and 101% inclusive (actual: "
              + totalWeight.stripTrailingZeros().toPlainString()
              + "%)");
    }
  }
}
