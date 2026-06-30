package com.portfolio.performance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared constants for portfolio return calculations.
 */
public final class CalculationConstants {

  /** Percentages are stored with two decimal places. */
  public static final int PERCENTAGE_SCALE = 2;

  /** Attribution contributions use three decimal places. */
  public static final int ATTRIBUTION_SCALE = 3;

  /** Minimum acceptable total portfolio weight (inclusive). */
  public static final BigDecimal MIN_TOTAL_WEIGHT_PCT = new BigDecimal("99");

  /** Maximum acceptable total portfolio weight (inclusive). */
  public static final BigDecimal MAX_TOTAL_WEIGHT_PCT = new BigDecimal("101");

  /** Intermediate division uses higher precision to avoid rounding drift. */
  public static final int DIVISION_SCALE = 10;

  public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

  /** Multiplier used when converting a ratio to a percentage. */
  public static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  /** Benchmark deviation above this threshold triggers manual review (in percent points). */
  public static final BigDecimal BENCHMARK_DEVIATION_THRESHOLD = new BigDecimal("5");

  /**
   * Net cash flow above this fraction of begin market value triggers manual review.
   * Example: 0.20 means 20%.
   */
  public static final BigDecimal CASH_FLOW_THRESHOLD_RATIO = new BigDecimal("0.20");

  private CalculationConstants() {
    // Utility class — no instances.
  }
}
