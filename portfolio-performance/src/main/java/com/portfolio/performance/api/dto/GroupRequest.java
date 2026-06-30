package com.portfolio.performance.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * A single asset group within an attribution request.
 *
 * @param groupName asset group label (e.g. Equity, Fixed Income)
 * @param weightPct portfolio weight for this group, in percent
 * @param returnPct primary return for the period, in percent
 * @param fallbackReturnPct alternate return used when {@code returnPct} is absent
 */
public record GroupRequest(
    @NotBlank String groupName,
    @NotNull BigDecimal weightPct,
    BigDecimal returnPct,
    BigDecimal fallbackReturnPct) {}
