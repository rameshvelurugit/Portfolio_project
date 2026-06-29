package com.portfolio.performance.application.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ReturnCalculatorTest {

  private ReturnCalculator calculator;

  @BeforeEach
  void setUp() {
    calculator = new ReturnCalculator();
  }

  @Test
  void calculatePortfolioReturnPct_matchesAssignmentSample() {
    BigDecimal result =
        calculator.calculatePortfolioReturnPct(
            new BigDecimal("1000000"),
            new BigDecimal("1035000"),
            new BigDecimal("10000"));

    assertEquals(new BigDecimal("2.50"), result);
  }

  @Test
  void calculatePortfolioReturnPct_whenBothValuesAreZero_returnsZero() {
    BigDecimal result =
        calculator.calculatePortfolioReturnPct(
            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5000"));

    assertEquals(new BigDecimal("0.00"), result);
  }

  @ParameterizedTest
  @MethodSource("portfolioReturnCases")
  void calculatePortfolioReturnPct_handlesVariousInputs(
      BigDecimal begin, BigDecimal end, BigDecimal cashFlow, BigDecimal expected) {

    BigDecimal result = calculator.calculatePortfolioReturnPct(begin, end, cashFlow);
    assertEquals(expected, result);
  }

  private static Stream<Arguments> portfolioReturnCases() {
    return Stream.of(
        Arguments.of(
            new BigDecimal("1000000"),
            new BigDecimal("1000000"),
            BigDecimal.ZERO,
            new BigDecimal("0.00")),
        Arguments.of(
            new BigDecimal("1000000"),
            new BigDecimal("1020000"),
            BigDecimal.ZERO,
            new BigDecimal("2.00")),
        Arguments.of(
            new BigDecimal("1000000"),
            new BigDecimal("1010000"),
            new BigDecimal("-5000"),
            new BigDecimal("1.50")),
        Arguments.of(
            new BigDecimal("333333"),
            new BigDecimal("444444"),
            new BigDecimal("11111"),
            new BigDecimal("30.00")));
  }

  @Test
  void calculateExcessReturnPct_subtractsBenchmarkFromPortfolioReturn() {
    BigDecimal excess =
        calculator.calculateExcessReturnPct(new BigDecimal("2.50"), new BigDecimal("1.80"));

    assertEquals(new BigDecimal("0.70"), excess);
  }

  @Test
  void calculateExcessReturnPct_roundsToTwoDecimalPlaces() {
    BigDecimal excess =
        calculator.calculateExcessReturnPct(new BigDecimal("3.335"), new BigDecimal("1.111"));

    assertEquals(new BigDecimal("2.22"), excess);
  }
}
