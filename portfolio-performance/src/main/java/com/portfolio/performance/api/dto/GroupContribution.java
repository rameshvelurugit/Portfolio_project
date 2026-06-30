package com.portfolio.performance.api.dto;

import com.portfolio.performance.domain.PricingMode;
import java.math.BigDecimal;

/**
 * Calculated contribution for a single asset group.
 *
 * @param groupName asset group label echoed from the request
 * @param weightPct portfolio weight echoed from the request
 * @param effectiveReturnPct return value used in the calculation; {@code null} when no data
 * @param contributionPct group's contribution to total return (scale 3)
 * @param pricingMode how the effective return was sourced; {@code null} when no return data
 */
public record GroupContribution(
    String groupName,
    BigDecimal weightPct,
    BigDecimal effectiveReturnPct,
    BigDecimal contributionPct,
    PricingMode pricingMode) {}
