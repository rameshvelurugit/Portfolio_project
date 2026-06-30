package com.portfolio.performance.application.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.portfolio.performance.api.dto.AttributionRequest;
import com.portfolio.performance.api.dto.GroupRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributionValidatorTest {

  private AttributionValidator validator;

  @BeforeEach
  void setUp() {
    validator = new AttributionValidator();
  }

  @Test
  void validate_whenWeightsSumTo100_returnsValid() {
    ValidationResult result = validator.validate(validRequest());

    assertTrue(result.isValid());
  }

  @Test
  void validate_whenTotalWeightIs80_returnsInvalid() {
    AttributionRequest request =
        validRequestBuilder()
            .groups(
                List.of(
                    new GroupRequest("Equity", new BigDecimal("50"), new BigDecimal("2.5"), null),
                    new GroupRequest(
                        "Fixed Income", new BigDecimal("30"), new BigDecimal("0.8"), null)))
            .build();

    ValidationResult result = validator.validate(request);

    assertFalse(result.isValid());
    assertTrue(result.reasons().get(0).contains("total weight must be between 99% and 101%"));
  }

  @Test
  void validate_whenGroupsIsEmpty_returnsInvalid() {
    AttributionRequest request = validRequestBuilder().groups(List.of()).build();

    ValidationResult result = validator.validate(request);

    assertFalse(result.isValid());
    assertEquals("groups must contain at least one asset group", result.reasons().get(0));
  }

  @Test
  void validate_whenGroupNameIsBlank_returnsInvalid() {
    AttributionRequest request =
        validRequestBuilder()
            .groups(
                List.of(
                    new GroupRequest(" ", new BigDecimal("60"), new BigDecimal("2.5"), null),
                    new GroupRequest(
                        "Fixed Income", new BigDecimal("30"), new BigDecimal("0.8"), null),
                    new GroupRequest("Cash", new BigDecimal("10"), new BigDecimal("0.1"), null)))
            .build();

    ValidationResult result = validator.validate(request);

    assertFalse(result.isValid());
    assertEquals("groupName is required for every asset group", result.reasons().get(0));
  }

  @Test
  void validate_whenWeightIsNegative_returnsInvalid() {
    AttributionRequest request =
        validRequestBuilder()
            .groups(
                List.of(
                    new GroupRequest("Equity", new BigDecimal("-10"), new BigDecimal("2.5"), null),
                    new GroupRequest(
                        "Fixed Income", new BigDecimal("60"), new BigDecimal("0.8"), null),
                    new GroupRequest("Cash", new BigDecimal("50"), new BigDecimal("0.1"), null)))
            .build();

    ValidationResult result = validator.validate(request);

    assertFalse(result.isValid());
    assertTrue(result.reasons().get(0).contains("weightPct must be non-negative"));
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
