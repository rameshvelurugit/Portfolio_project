package com.portfolio.performance.application.service;

import static com.portfolio.performance.domain.CalculationConstants.ATTRIBUTION_SCALE;
import static com.portfolio.performance.domain.CalculationConstants.ROUNDING_MODE;

import com.portfolio.performance.api.dto.AttributionRequest;
import com.portfolio.performance.api.dto.AttributionResponse;
import com.portfolio.performance.api.dto.GroupContribution;
import com.portfolio.performance.api.dto.GroupRequest;
import com.portfolio.performance.application.calculation.AttributionCalculator;
import com.portfolio.performance.application.idempotency.IdempotencyStore;
import com.portfolio.performance.application.validation.AttributionValidator;
import com.portfolio.performance.application.validation.ValidationResult;
import com.portfolio.performance.domain.AttributionStatus;
import com.portfolio.performance.domain.PricingMode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates validation, calculation, status classification, and idempotency for attribution.
 */
@Service
public class AttributionService {

  private static final Logger log = LoggerFactory.getLogger(AttributionService.class);

  private final AttributionValidator validator;
  private final AttributionCalculator calculator;
  private final IdempotencyStore idempotencyStore;
  private final Clock clock;

  public AttributionService(
      AttributionValidator validator,
      AttributionCalculator calculator,
      IdempotencyStore idempotencyStore,
      Clock clock) {
    this.validator = validator;
    this.calculator = calculator;
    this.idempotencyStore = idempotencyStore;
    this.clock = clock;
  }

  /**
   * Processes an attribution request and returns a fully populated response.
   *
   * @param request validated request payload
   * @return response including contributions, status, and any warnings
   */
  public AttributionResponse calculateAttribution(AttributionRequest request) {
    Optional<AttributionResponse> cached = idempotencyStore.findCached(request);
    if (cached.isPresent()) {
      return cached.get();
    }

    log.info(
        "Processing attribution request requestId={} portfolioId={}",
        request.requestId(),
        request.portfolioId());

    ValidationResult validationResult = validator.validate(request);

    if (!validationResult.isValid()) {
      AttributionResponse response =
          buildInvalidInputResponse(request, validationResult.reasons(), Instant.now(clock));
      idempotencyStore.store(request, response);
      return response;
    }

    List<String> warnings = new ArrayList<>();
    List<GroupContribution> contributions = new ArrayList<>();
    int missingCount = 0;

    for (GroupRequest group : request.groups()) {
      GroupContribution contribution = processGroup(group, warnings);
      contributions.add(contribution);
      if (contribution.effectiveReturnPct() == null) {
        missingCount++;
      }
    }

    AttributionStatus status = determineStatus(missingCount);
    BigDecimal totalContributionPct = sumContributions(contributions);
    boolean degraded = status == AttributionStatus.DEGRADED;

    if (status == AttributionStatus.DEGRADED) {
      log.warn(
          "Degraded attribution processing for requestId={} portfolioId={}",
          request.requestId(),
          request.portfolioId());
    }

    log.info(
        "Attribution completed requestId={} status={} totalContributionPct={}",
        request.requestId(),
        status,
        totalContributionPct);

    AttributionResponse response =
        buildResponse(
            request,
            totalContributionPct,
            status,
            degraded,
            warnings,
            contributions,
            Instant.now(clock));

    idempotencyStore.store(request, response);
    return response;
  }

  private GroupContribution processGroup(GroupRequest group, List<String> warnings) {
    if (group.returnPct() != null) {
      return buildContribution(
          group,
          group.returnPct(),
          calculator.calculateContributionPct(group.weightPct(), group.returnPct()),
          PricingMode.PRIMARY);
    }

    if (group.fallbackReturnPct() != null) {
      warnings.add("fallback return used for group: " + group.groupName());
      log.info("Fallback return used for group: {}", group.groupName());
      return buildContribution(
          group,
          group.fallbackReturnPct(),
          calculator.calculateContributionPct(group.weightPct(), group.fallbackReturnPct()),
          PricingMode.FALLBACK_USED);
    }

    warnings.add("missing return data for group: " + group.groupName());
    return buildContribution(group, null, calculator.zeroContribution(), null);
  }

  private GroupContribution buildContribution(
      GroupRequest group,
      BigDecimal effectiveReturnPct,
      BigDecimal contributionPct,
      PricingMode pricingMode) {
    return new GroupContribution(
        group.groupName(),
        group.weightPct(),
        effectiveReturnPct,
        contributionPct,
        pricingMode);
  }

  private AttributionStatus determineStatus(int missingCount) {
    if (missingCount > 1) {
      return AttributionStatus.REVIEW_REQUIRED;
    }
    if (missingCount == 1) {
      return AttributionStatus.DEGRADED;
    }
    return AttributionStatus.VALID;
  }

  private BigDecimal sumContributions(List<GroupContribution> contributions) {
    return contributions.stream()
        .map(GroupContribution::contributionPct)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(ATTRIBUTION_SCALE, ROUNDING_MODE);
  }

  private AttributionResponse buildInvalidInputResponse(
      AttributionRequest request, List<String> warnings, Instant processedAt) {
    return buildResponse(
        request,
        null,
        AttributionStatus.INVALID_INPUT,
        false,
        warnings,
        List.of(),
        processedAt);
  }

  private AttributionResponse buildResponse(
      AttributionRequest request,
      BigDecimal totalContributionPct,
      AttributionStatus status,
      boolean degraded,
      List<String> warnings,
      List<GroupContribution> groups,
      Instant processedAt) {
    return new AttributionResponse(
        request.requestId(),
        request.portfolioId(),
        request.valuationDate(),
        totalContributionPct,
        status,
        degraded,
        List.copyOf(warnings),
        List.copyOf(groups),
        processedAt);
  }
}
