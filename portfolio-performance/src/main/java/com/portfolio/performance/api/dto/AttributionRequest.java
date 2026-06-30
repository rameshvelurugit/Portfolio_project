package com.portfolio.performance.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * Request payload for the portfolio performance attribution endpoint.
 *
 * @param requestId idempotency key for duplicate detection
 * @param portfolioId unique portfolio identifier
 * @param valuationDate date the attribution applies to
 * @param groups asset groups with weights and returns
 * @param requestedBy identifier of the user or system that submitted the request
 */
public record AttributionRequest(
    @NotBlank String requestId,
    @NotBlank String portfolioId,
    @NotNull LocalDate valuationDate,
    @NotEmpty @Valid List<GroupRequest> groups,
    @NotBlank String requestedBy) {}
