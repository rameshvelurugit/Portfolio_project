package com.portfolio.performance.application.calculation;

import static com.portfolio.performance.domain.CalculationConstants.DIVISION_SCALE;
import static com.portfolio.performance.domain.CalculationConstants.ONE_HUNDRED;
import static com.portfolio.performance.domain.CalculationConstants.PERCENTAGE_SCALE;
import static com.portfolio.performance.domain.CalculationConstants.ROUNDING_MODE;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Performs portfolio return math using {@link BigDecimal} for precision.
 */
@Component
public class ReturnCalculator {

  private static final BigDecimal ZERO = BigDecimal.ZERO;

  /**
   * Calculates the portfolio return percentage for the given market values and cash flow.
   *
   * <p>Formula: {@code ((end - begin - netCashFlow) / begin) * 100}
   *
   * <p>When both begin and end values are zero, the return is defined as {@code 0.00}.
   *
   * @param beginMarketValue starting market value
   * @param endMarketValue ending market value
   * @param netCashFlow net cash flow during the period
   * @return portfolio return as a percentage with scale 2
   */
  public BigDecimal calculatePortfolioReturnPct(
      BigDecimal beginMarketValue, BigDecimal endMarketValue, BigDecimal netCashFlow) {

    if (isZero(beginMarketValue) && isZero(endMarketValue)) {
      return ZERO.setScale(PERCENTAGE_SCALE, ROUNDING_MODE);
    }

    BigDecimal gain =
        endMarketValue.subtract(beginMarketValue).subtract(netCashFlow);

    return gain
        .divide(beginMarketValue, DIVISION_SCALE, ROUNDING_MODE)
        .multiply(ONE_HUNDRED)
        .setScale(PERCENTAGE_SCALE, ROUNDING_MODE);
  }

  /**
   * Calculates how much the portfolio return exceeds the benchmark return.
   *
   * @param portfolioReturnPct calculated portfolio return
   * @param benchmarkReturnPct benchmark return from the request
   * @return excess return with scale 2
   */
  public BigDecimal calculateExcessReturnPct(
      BigDecimal portfolioReturnPct, BigDecimal benchmarkReturnPct) {
    return portfolioReturnPct
        .subtract(benchmarkReturnPct)
        .setScale(PERCENTAGE_SCALE, ROUNDING_MODE);
  }

  private boolean isZero(BigDecimal value) {
    return value.compareTo(ZERO) == 0;
  }
}
