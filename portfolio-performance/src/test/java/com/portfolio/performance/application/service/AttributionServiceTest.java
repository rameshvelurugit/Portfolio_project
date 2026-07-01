package com.portfolio.performance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.portfolio.performance.api.dto.AttributionRequest;
import com.portfolio.performance.api.dto.AttributionResponse;
import com.portfolio.performance.api.dto.GroupRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portfolio.performance.application.calculation.AttributionCalculator;
import com.portfolio.performance.application.idempotency.IdempotencyStore;
import com.portfolio.performance.application.idempotency.InMemoryIdempotencyStore;
import com.portfolio.performance.application.idempotency.RequestHasher;
import com.portfolio.performance.application.validation.AttributionValidator;
import com.portfolio.performance.domain.AttributionStatus;
import com.portfolio.performance.domain.PricingMode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributionServiceTest {

  private static final Instant FIXED_INSTANT = Instant.parse("2026-06-14T10:30:00Z");

  private AttributionService service;

  @BeforeEach
  void setUp() {
    Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    IdempotencyStore idempotencyStore = new InMemoryIdempotencyStore(new RequestHasher(objectMapper));
    service =
        new AttributionService(
            new AttributionValidator(),
            new AttributionCalculator(),
            idempotencyStore,
            fixedClock);
  }

  @Test
  void calculateAttribution_whenAllPrimary_returnsValid() {
    AttributionResponse response = service.calculateAttribution(validRequest());

    assertEquals(AttributionStatus.VALID, response.status());
    assertFalse(response.degraded());
    assertEquals(new BigDecimal("1.750"), response.totalContributionPct());
    assertTrue(response.warnings().isEmpty());
    assertEquals(FIXED_INSTANT, response.processedAt());
    assertEquals(new BigDecimal("1.500"), response.groups().get(0).contributionPct());
    assertEquals(PricingMode.PRIMARY, response.groups().get(0).pricingMode());
  }

  @Test
  void calculateAttribution_whenTotalWeightIsInvalid_returnsInvalidInput() {
    AttributionRequest request =
        validRequestBuilder()
            .groups(
                List.of(
                    new GroupRequest("Equity", new BigDecimal("50"), new BigDecimal("2.5"), null),
                    new GroupRequest(
                        "Fixed Income", new BigDecimal("30"), new BigDecimal("0.8"), null)))
            .build();

    AttributionResponse response = service.calculateAttribution(request);

    assertEquals(AttributionStatus.INVALID_INPUT, response.status());
    assertFalse(response.degraded());
    assertNull(response.totalContributionPct());
    assertTrue(response.groups().isEmpty());
  }

  @Test
  void calculateAttribution_whenFallbackUsed_returnsValidWithWarning() {
    AttributionRequest request =
        validRequestBuilder()
            .groups(
                List.of(
                    new GroupRequest("Equity", new BigDecimal("60"), null, new BigDecimal("2.0")),
                    new GroupRequest(
                        "Fixed Income", new BigDecimal("30"), new BigDecimal("0.8"), null),
                    new GroupRequest("Cash", new BigDecimal("10"), new BigDecimal("0.1"), null)))
            .build();

    AttributionResponse response = service.calculateAttribution(request);

    assertEquals(AttributionStatus.VALID, response.status());
    assertEquals(PricingMode.FALLBACK_USED, response.groups().get(0).pricingMode());
    assertEquals("fallback return used for group: Equity", response.warnings().get(0));
  }

  @Test
  void calculateAttribution_whenExactlyOneGroupMissing_returnsDegraded() {
    AttributionRequest request =
        validRequestBuilder()
            .groups(
                List.of(
                    new GroupRequest("Equity", new BigDecimal("60"), new BigDecimal("2.5"), null),
                    new GroupRequest("Fixed Income", new BigDecimal("30"), null, null),
                    new GroupRequest("Cash", new BigDecimal("10"), new BigDecimal("0.1"), null)))
            .build();

    AttributionResponse response = service.calculateAttribution(request);

    assertEquals(AttributionStatus.DEGRADED, response.status());
    assertTrue(response.degraded());
    assertEquals(new BigDecimal("0.000"), response.groups().get(1).contributionPct());
    assertNull(response.groups().get(1).effectiveReturnPct());
    assertNull(response.groups().get(1).pricingMode());
    assertTrue(
        response.warnings().stream()
            .anyMatch(w -> w.contains("missing return data for group: Fixed Income")));
  }

  @Test
  void calculateAttribution_whenTwoGroupsMissing_returnsReviewRequired() {
    AttributionRequest request =
        validRequestBuilder()
            .groups(
                List.of(
                    new GroupRequest("Equity", new BigDecimal("60"), null, null),
                    new GroupRequest("Fixed Income", new BigDecimal("30"), null, null),
                    new GroupRequest("Cash", new BigDecimal("10"), new BigDecimal("0.1"), null)))
            .build();

    AttributionResponse response = service.calculateAttribution(request);

    assertEquals(AttributionStatus.REVIEW_REQUIRED, response.status());
    assertFalse(response.degraded());
    assertEquals(2, response.warnings().stream().filter(w -> w.contains("missing return")).count());
  }

  @Test
  void calculateAttribution_whenDuplicateRequestId_returnsCachedResponse() {
    AttributionRequest request = validRequest();

    AttributionResponse first = service.calculateAttribution(request);
    AttributionResponse second = service.calculateAttribution(request);

    assertEquals(first, second);
    assertEquals(FIXED_INSTANT, second.processedAt());
  }

  private static AttributionRequest validRequest() {
    return validRequestBuilder().build();
  }

  private static ValidRequestBuilder validRequestBuilder() {
    return new ValidRequestBuilder();
  }

  private static final class ValidRequestBuilder {
    private String requestId = "REQ-ATTR-001";
    private String portfolioId = "PF-1001";
    private LocalDate valuationDate = LocalDate.of(2026, 6, 14);
    private List<GroupRequest> groups =
        List.of(
            new GroupRequest("Equity", new BigDecimal("60"), new BigDecimal("2.5"), null),
            new GroupRequest("Fixed Income", new BigDecimal("30"), new BigDecimal("0.8"), null),
            new GroupRequest("Cash", new BigDecimal("10"), new BigDecimal("0.1"), null));
    private String requestedBy = "advisor01";

    ValidRequestBuilder groups(List<GroupRequest> groups) {
      this.groups = groups;
      return this;
    }

    AttributionRequest build() {
      return new AttributionRequest(
          requestId, portfolioId, valuationDate, groups, requestedBy);
    }
  }
}
