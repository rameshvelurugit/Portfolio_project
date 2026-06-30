package com.portfolio.performance.application.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributionCalculatorTest {

  private AttributionCalculator calculator;

  @BeforeEach
  void setUp() {
    calculator = new AttributionCalculator();
  }

  @Test
  void calculateContributionPct_assignmentSampleEquity_returns1500() {
    BigDecimal contribution =
        calculator.calculateContributionPct(new BigDecimal("60"), new BigDecimal("2.5"));

    assertEquals(new BigDecimal("1.500"), contribution);
  }

  @Test
  void calculateContributionPct_assignmentSampleFixedIncome_returns0240() {
    BigDecimal contribution =
        calculator.calculateContributionPct(new BigDecimal("30"), new BigDecimal("0.8"));

    assertEquals(new BigDecimal("0.240"), contribution);
  }

  @Test
  void calculateContributionPct_assignmentSampleCash_returns0010() {
    BigDecimal contribution =
        calculator.calculateContributionPct(new BigDecimal("10"), new BigDecimal("0.1"));

    assertEquals(new BigDecimal("0.010"), contribution);
  }

  @Test
  void zeroContribution_returnsScaledZero() {
    assertEquals(new BigDecimal("0.000"), calculator.zeroContribution());
  }
}
