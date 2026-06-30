package com.portfolio.performance.application.calculation;

import static com.portfolio.performance.domain.CalculationConstants.ATTRIBUTION_SCALE;
import static com.portfolio.performance.domain.CalculationConstants.DIVISION_SCALE;
import static com.portfolio.performance.domain.CalculationConstants.ONE_HUNDRED;
import static com.portfolio.performance.domain.CalculationConstants.ROUNDING_MODE;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Performs portfolio attribution contribution math using {@link BigDecimal} for precision.
 */
@Component
public class AttributionCalculator {

  private static final BigDecimal ZERO = BigDecimal.ZERO;

  /**
   * Calculates a group's contribution to total portfolio return.
   *
   * <p>Formula: {@code (weightPct × effectiveReturnPct) / 100}
   *
   * @param weightPct portfolio weight for the group, in percent
   * @param effectiveReturnPct return used for the group, in percent
   * @return contribution with scale 3
   */
  public BigDecimal calculateContributionPct(BigDecimal weightPct, BigDecimal effectiveReturnPct) {
    return weightPct
        .multiply(effectiveReturnPct)
        .divide(ONE_HUNDRED, DIVISION_SCALE, ROUNDING_MODE)
        .setScale(ATTRIBUTION_SCALE, ROUNDING_MODE);
  }

  /**
   * Returns a zero contribution with the attribution scale applied.
   *
   * @return {@code 0.000}
   */
  public BigDecimal zeroContribution() {
    return ZERO.setScale(ATTRIBUTION_SCALE, ROUNDING_MODE);
  }
}
