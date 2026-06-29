package com.portfolio.performance.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for the daily portfolio return endpoint.
 *
 * @param portfolioId unique portfolio identifier
 * @param valuationDate date the return is calculated for
 * @param beginMarketValue market value at the start of the period
 * @param endMarketValue market value at the end of the period
 * @param netCashFlow net cash added (positive) or withdrawn (negative) during the period
 * @param benchmarkReturnPct benchmark return for the same period, in percent
 * @param currency ISO currency code (e.g. USD)
 * @param requestedBy identifier of the user or system that submitted the request
 */
public record DailyReturnRequest(
    @NotBlank String portfolioId,
    @NotNull LocalDate valuationDate,
    @NotNull BigDecimal beginMarketValue,
    @NotNull BigDecimal endMarketValue,
    @NotNull BigDecimal netCashFlow,
    @NotNull BigDecimal benchmarkReturnPct,
    @NotBlank String currency,
    @NotBlank String requestedBy) {}
