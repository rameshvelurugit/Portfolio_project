package com.portfolio.performance.application.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.portfolio.performance.api.dto.DailyReturnRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DailyReturnValidatorTest {

  private DailyReturnValidator validator;

  @BeforeEach
  void setUp() {
    validator = new DailyReturnValidator();
  }

  @Test
  void validate_whenRequestIsValid_returnsNoReasons() {
    ValidationResult result = validator.validate(validRequestBuilder().build());

    assertTrue(result.isValid());
    assertTrue(result.reasons().isEmpty());
  }

  @Test
  void validate_whenBeginMarketValueIsNegative_returnsInvalidInputReason() {
    DailyReturnRequest request =
        validRequestBuilder().beginMarketValue(new BigDecimal("-1")).build();

    ValidationResult result = validator.validate(request);

    assertFalse(result.isValid());
    assertEquals(1, result.reasons().size());
    assertEquals("beginMarketValue must be non-negative", result.reasons().get(0));
  }

  @Test
  void validate_whenEndMarketValueIsNegative_returnsInvalidInputReason() {
    DailyReturnRequest request =
        validRequestBuilder().endMarketValue(new BigDecimal("-1")).build();

    ValidationResult result = validator.validate(request);

    assertFalse(result.isValid());
    assertEquals("endMarketValue must be non-negative", result.reasons().get(0));
  }

  @Test
  void validate_whenCurrencyIsBlank_returnsInvalidInputReason() {
    DailyReturnRequest request = validRequestBuilder().currency("  ").build();

    ValidationResult result = validator.validate(request);

    assertFalse(result.isValid());
    assertEquals("currency is required", result.reasons().get(0));
  }

  @Test
  void validate_whenBeginIsZeroAndEndIsNonZero_returnsInvalidInputReason() {
    DailyReturnRequest request =
        validRequestBuilder()
            .beginMarketValue(BigDecimal.ZERO)
            .endMarketValue(new BigDecimal("1000"))
            .build();

    ValidationResult result = validator.validate(request);

    assertFalse(result.isValid());
    assertEquals(
        "cannot compute return when beginMarketValue is zero and endMarketValue is non-zero",
        result.reasons().get(0));
  }

  @Test
  void validate_whenMultipleRulesFail_returnsAllReasons() {
    DailyReturnRequest request =
        validRequestBuilder()
            .beginMarketValue(new BigDecimal("-1"))
            .endMarketValue(new BigDecimal("-2"))
            .currency("")
            .build();

    ValidationResult result = validator.validate(request);

    assertFalse(result.isValid());
    assertEquals(3, result.reasons().size());
  }

  private static ValidRequestBuilder validRequestBuilder() {
    return new ValidRequestBuilder();
  }

  private static final class ValidRequestBuilder {
    private String portfolioId = "PF-1001";
    private LocalDate valuationDate = LocalDate.of(2026, 6, 14);
    private BigDecimal beginMarketValue = new BigDecimal("1000000");
    private BigDecimal endMarketValue = new BigDecimal("1035000");
    private BigDecimal netCashFlow = new BigDecimal("10000");
    private BigDecimal benchmarkReturnPct = new BigDecimal("1.8");
    private String currency = "USD";
    private String requestedBy = "advisor01";

    ValidRequestBuilder beginMarketValue(BigDecimal value) {
      this.beginMarketValue = value;
      return this;
    }

    ValidRequestBuilder endMarketValue(BigDecimal value) {
      this.endMarketValue = value;
      return this;
    }

    ValidRequestBuilder currency(String value) {
      this.currency = value;
      return this;
    }

    DailyReturnRequest build() {
      return new DailyReturnRequest(
          portfolioId,
          valuationDate,
          beginMarketValue,
          endMarketValue,
          netCashFlow,
          benchmarkReturnPct,
          currency,
          requestedBy);
    }
  }
}
