package com.portfolio.performance.api.dto;

import com.portfolio.performance.domain.CalculationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Response payload for the daily portfolio return endpoint.
 *
 * @param portfolioId portfolio that was evaluated
 * @param valuationDate date the return applies to
 * @param portfolioReturnPct calculated portfolio return in percent
 * @param benchmarkReturnPct benchmark return echoed from the request
 * @param excessReturnPct portfolio return minus benchmark return
 * @param status outcome of the calculation
 * @param reasons human-readable explanations when review or rejection is needed
 * @param processedAt UTC timestamp when the response was produced
 */
public record DailyReturnResponse(
    String portfolioId,
    LocalDate valuationDate,
    BigDecimal portfolioReturnPct,
    BigDecimal benchmarkReturnPct,
    BigDecimal excessReturnPct,
    CalculationStatus status,
    List<String> reasons,
    Instant processedAt) {}
