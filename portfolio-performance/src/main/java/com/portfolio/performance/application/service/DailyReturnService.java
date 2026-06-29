package com.portfolio.performance.application.service;

import static com.portfolio.performance.domain.CalculationConstants.BENCHMARK_DEVIATION_THRESHOLD;
import static com.portfolio.performance.domain.CalculationConstants.CASH_FLOW_THRESHOLD_RATIO;
import static com.portfolio.performance.domain.CalculationConstants.PERCENTAGE_SCALE;
import static com.portfolio.performance.domain.CalculationConstants.ROUNDING_MODE;

import com.portfolio.performance.api.dto.DailyReturnRequest;
import com.portfolio.performance.api.dto.DailyReturnResponse;
import com.portfolio.performance.application.calculation.ReturnCalculator;
import com.portfolio.performance.application.validation.DailyReturnValidator;
import com.portfolio.performance.application.validation.ValidationResult;
import com.portfolio.performance.domain.CalculationStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Orchestrates validation, calculation, and status classification for daily returns.
 */
@Service
public class DailyReturnService {

  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final DailyReturnValidator validator;
  private final ReturnCalculator calculator;
  private final Clock clock;

  public DailyReturnService(
      DailyReturnValidator validator, ReturnCalculator calculator, Clock clock) {
    this.validator = validator;
    this.calculator = calculator;
    this.clock = clock;
  }

  /**
   * Processes a daily return request and returns a fully populated response.
   *
   * @param request validated request payload
   * @return response including return percentages, status, and any reasons
   */
  public DailyReturnResponse calculateDailyReturn(DailyReturnRequest request) {
    ValidationResult validationResult = validator.validate(request);

    if (!validationResult.isValid()) {
      return buildResponse(
          request,
          null,
          null,
          CalculationStatus.INVALID_INPUT,
          validationResult.reasons(),
          Instant.now(clock));
    }

    BigDecimal portfolioReturnPct =
        calculator.calculatePortfolioReturnPct(
            request.beginMarketValue(), request.endMarketValue(), request.netCashFlow());

    BigDecimal excessReturnPct =
        calculator.calculateExcessReturnPct(
            portfolioReturnPct, request.benchmarkReturnPct());

    List<String> reviewReasons = findReviewReasons(request, portfolioReturnPct);

    CalculationStatus status =
        reviewReasons.isEmpty() ? CalculationStatus.VALID : CalculationStatus.REVIEW_REQUIRED;

    return buildResponse(
        request,
        portfolioReturnPct,
        excessReturnPct,
        status,
        reviewReasons,
        Instant.now(clock));
  }

  private List<String> findReviewReasons(
      DailyReturnRequest request, BigDecimal portfolioReturnPct) {

    List<String> reasons = new ArrayList<>();

    if (exceedsBenchmarkDeviation(portfolioReturnPct, request.benchmarkReturnPct())) {
      reasons.add("portfolio return deviates from benchmark by more than 5%");
    }

    if (exceedsCashFlowThreshold(request.beginMarketValue(), request.netCashFlow())) {
      reasons.add("net cash flow exceeds 20% of begin market value");
    }

    return reasons;
  }

  private boolean exceedsBenchmarkDeviation(
      BigDecimal portfolioReturnPct, BigDecimal benchmarkReturnPct) {
    BigDecimal deviation =
        portfolioReturnPct
            .subtract(benchmarkReturnPct)
            .abs()
            .setScale(PERCENTAGE_SCALE, ROUNDING_MODE);
    return deviation.compareTo(BENCHMARK_DEVIATION_THRESHOLD) > 0;
  }

  private boolean exceedsCashFlowThreshold(BigDecimal beginMarketValue, BigDecimal netCashFlow) {
    BigDecimal threshold =
        beginMarketValue
            .multiply(CASH_FLOW_THRESHOLD_RATIO)
            .abs()
            .setScale(PERCENTAGE_SCALE, ROUNDING_MODE);
    return netCashFlow.abs().compareTo(threshold) > 0;
  }

  private DailyReturnResponse buildResponse(
      DailyReturnRequest request,
      BigDecimal portfolioReturnPct,
      BigDecimal excessReturnPct,
      CalculationStatus status,
      List<String> reasons,
      Instant processedAt) {

    return new DailyReturnResponse(
        request.portfolioId(),
        request.valuationDate(),
        portfolioReturnPct,
        request.benchmarkReturnPct(),
        excessReturnPct,
        status,
        List.copyOf(reasons),
        processedAt);
  }
}
