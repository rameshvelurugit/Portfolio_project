package com.portfolio.performance.api.dto;

import com.portfolio.performance.domain.AttributionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Response payload for the portfolio performance attribution endpoint.
 *
 * @param requestId idempotency key echoed from the request
 * @param portfolioId portfolio identifier echoed from the request
 * @param valuationDate valuation date echoed from the request
 * @param totalContributionPct sum of group contributions (scale 3); {@code null} when invalid
 * @param status outcome of the attribution calculation
 * @param degraded {@code true} only when status is {@link AttributionStatus#DEGRADED}
 * @param warnings human-readable messages for fallbacks, missing data, or validation failures
 * @param groups per-group contribution details
 * @param processedAt UTC timestamp when the response was produced
 */
public record AttributionResponse(
    String requestId,
    String portfolioId,
    LocalDate valuationDate,
    BigDecimal totalContributionPct,
    AttributionStatus status,
    boolean degraded,
    List<String> warnings,
    List<GroupContribution> groups,
    Instant processedAt) {}
