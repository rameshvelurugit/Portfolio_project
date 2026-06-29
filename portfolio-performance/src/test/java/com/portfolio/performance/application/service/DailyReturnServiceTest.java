package com.portfolio.performance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.portfolio.performance.api.dto.DailyReturnRequest;
import com.portfolio.performance.api.dto.DailyReturnResponse;
import com.portfolio.performance.application.calculation.ReturnCalculator;
import com.portfolio.performance.application.validation.DailyReturnValidator;
import com.portfolio.performance.domain.CalculationStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DailyReturnServiceTest {

  private static final Instant FIXED_INSTANT = Instant.parse("2026-06-14T10:30:00Z");

  private DailyReturnService service;

  @BeforeEach
  void setUp() {
    Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    service =
        new DailyReturnService(
            new DailyReturnValidator(), new ReturnCalculator(), fixedClock);
  }

  @Test
  void calculateDailyReturn_whenInputsAreValid_returnsValidStatus() {
    DailyReturnResponse response = service.calculateDailyReturn(validRequest());

    assertEquals(CalculationStatus.VALID, response.status());
    assertEquals(new BigDecimal("2.50"), response.portfolioReturnPct());
    assertEquals(new BigDecimal("1.8"), response.benchmarkReturnPct());
    assertEquals(new BigDecimal("0.70"), response.excessReturnPct());
    assertTrue(response.reasons().isEmpty());
    assertEquals(FIXED_INSTANT, response.processedAt());
  }

  @Test
  void calculateDailyReturn_whenBenchmarkDeviationExceedsThreshold_returnsReviewRequired() {
    DailyReturnRequest request =
        validRequestBuilder().benchmarkReturnPct(new BigDecimal("-3.0")).build();

    DailyReturnResponse response = service.calculateDailyReturn(request);

    assertEquals(CalculationStatus.REVIEW_REQUIRED, response.status());
    assertEquals(
        "portfolio return deviates from benchmark by more than 5%",
        response.reasons().get(0));
  }

  @Test
  void calculateDailyReturn_whenCashFlowExceedsThreshold_returnsReviewRequired() {
    DailyReturnRequest request =
        validRequestBuilder().netCashFlow(new BigDecimal("250000")).build();

    DailyReturnResponse response = service.calculateDailyReturn(request);

    assertEquals(CalculationStatus.REVIEW_REQUIRED, response.status());
    assertEquals(
        "net cash flow exceeds 20% of begin market value", response.reasons().get(1));
  }

  @Test
  void calculateDailyReturn_whenBeginMarketValueIsNegative_returnsInvalidInput() {
    DailyReturnRequest request =
        validRequestBuilder().beginMarketValue(new BigDecimal("-100")).build();

    DailyReturnResponse response = service.calculateDailyReturn(request);

    assertEquals(CalculationStatus.INVALID_INPUT, response.status());
    assertNull(response.portfolioReturnPct());
    assertNull(response.excessReturnPct());
    assertEquals("beginMarketValue must be non-negative", response.reasons().get(0));
  }

  @Test
  void calculateDailyReturn_whenBeginIsZeroAndEndIsNonZero_returnsInvalidInput() {
    DailyReturnRequest request =
        validRequestBuilder()
            .beginMarketValue(BigDecimal.ZERO)
            .endMarketValue(new BigDecimal("1000"))
            .build();

    DailyReturnResponse response = service.calculateDailyReturn(request);

    assertEquals(CalculationStatus.INVALID_INPUT, response.status());
  }

  @Test
  void calculateDailyReturn_whenBothMarketValuesAreZero_returnsZeroReturn() {
    DailyReturnRequest request =
        validRequestBuilder()
            .beginMarketValue(BigDecimal.ZERO)
            .endMarketValue(BigDecimal.ZERO)
            .netCashFlow(BigDecimal.ZERO)
            .build();

    DailyReturnResponse response = service.calculateDailyReturn(request);

    assertEquals(CalculationStatus.VALID, response.status());
    assertEquals(new BigDecimal("0.00"), response.portfolioReturnPct());
    assertEquals(new BigDecimal("-1.80"), response.excessReturnPct());
  }

  @Test
  void calculateDailyReturn_whenBothMarketValuesAreZeroAndCashFlowIsLarge_returnsReviewRequired() {
    DailyReturnRequest request =
        validRequestBuilder()
            .beginMarketValue(BigDecimal.ZERO)
            .endMarketValue(BigDecimal.ZERO)
            .netCashFlow(new BigDecimal("1000"))
            .build();

    DailyReturnResponse response = service.calculateDailyReturn(request);

    assertEquals(CalculationStatus.REVIEW_REQUIRED, response.status());
    assertEquals(new BigDecimal("0.00"), response.portfolioReturnPct());
    assertEquals(
        "net cash flow exceeds 20% of begin market value", response.reasons().get(0));
  }

  private static DailyReturnRequest validRequest() {
    return validRequestBuilder().build();
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

    ValidRequestBuilder netCashFlow(BigDecimal value) {
      this.netCashFlow = value;
      return this;
    }

    ValidRequestBuilder benchmarkReturnPct(BigDecimal value) {
      this.benchmarkReturnPct = value;
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
