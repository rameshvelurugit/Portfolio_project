package com.portfolio.performance.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.performance.api.dto.AttributionResponse;
import com.portfolio.performance.api.dto.GroupContribution;
import com.portfolio.performance.application.service.AttributionService;
import com.portfolio.performance.domain.AttributionStatus;
import com.portfolio.performance.domain.PricingMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PerformanceController.class)
class PerformanceControllerAttributionTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AttributionService attributionService;

  @MockBean private com.portfolio.performance.application.service.DailyReturnService dailyReturnService;

  @Test
  void calculateAttribution_whenRequestIsValid_returnsOkWithResponseBody() throws Exception {
    when(attributionService.calculateAttribution(any()))
        .thenReturn(
            new AttributionResponse(
                "REQ-ATTR-001",
                "PF-1001",
                LocalDate.of(2026, 6, 14),
                new BigDecimal("1.750"),
                AttributionStatus.VALID,
                false,
                List.of(),
                List.of(
                    new GroupContribution(
                        "Equity",
                        new BigDecimal("60"),
                        new BigDecimal("2.5"),
                        new BigDecimal("1.500"),
                        PricingMode.PRIMARY)),
                Instant.parse("2026-06-14T10:30:00Z")));

    mockMvc
        .perform(
            post("/api/performance/attribution")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "requestId": "REQ-ATTR-001",
                      "portfolioId": "PF-1001",
                      "valuationDate": "2026-06-14",
                      "requestedBy": "advisor01",
                      "groups": [
                        { "groupName": "Equity", "weightPct": 60, "returnPct": 2.5 },
                        { "groupName": "Fixed Income", "weightPct": 30, "returnPct": 0.8 },
                        { "groupName": "Cash", "weightPct": 10, "returnPct": 0.1 }
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestId").value("REQ-ATTR-001"))
        .andExpect(jsonPath("$.totalContributionPct").value(1.750))
        .andExpect(jsonPath("$.status").value("VALID"))
        .andExpect(jsonPath("$.degraded").value(false))
        .andExpect(jsonPath("$.warnings").isEmpty());
  }

  @Test
  void calculateAttribution_whenRequiredFieldIsMissing_returnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/performance/attribution")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "portfolioId": "PF-1001",
                      "valuationDate": "2026-06-14",
                      "requestedBy": "advisor01",
                      "groups": [
                        { "groupName": "Equity", "weightPct": 60, "returnPct": 2.5 }
                      ]
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("requestId"));
  }

  @Test
  void calculateAttribution_whenBusinessValidationFails_returnsOkWithInvalidInputStatus()
      throws Exception {
    when(attributionService.calculateAttribution(any()))
        .thenReturn(
            new AttributionResponse(
                "REQ-ATTR-002",
                "PF-1001",
                LocalDate.of(2026, 6, 14),
                null,
                AttributionStatus.INVALID_INPUT,
                false,
                List.of("total weight must be between 99% and 101% inclusive (actual: 80%)"),
                List.of(),
                Instant.parse("2026-06-14T10:30:00Z")));

    mockMvc
        .perform(
            post("/api/performance/attribution")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "requestId": "REQ-ATTR-002",
                      "portfolioId": "PF-1001",
                      "valuationDate": "2026-06-14",
                      "requestedBy": "advisor01",
                      "groups": [
                        { "groupName": "Equity", "weightPct": 50, "returnPct": 2.5 },
                        { "groupName": "Fixed Income", "weightPct": 30, "returnPct": 0.8 }
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.totalContributionPct").doesNotExist())
        .andExpect(jsonPath("$.warnings[0]").value("total weight must be between 99% and 101% inclusive (actual: 80%)"));
  }

  @Test
  void calculateAttribution_whenDegraded_returnsOkWithDegradedFlag() throws Exception {
    when(attributionService.calculateAttribution(any()))
        .thenReturn(
            new AttributionResponse(
                "REQ-ATTR-003",
                "PF-1001",
                LocalDate.of(2026, 6, 14),
                new BigDecimal("1.510"),
                AttributionStatus.DEGRADED,
                true,
                List.of("missing return data for group: Fixed Income"),
                List.of(),
                Instant.parse("2026-06-14T10:30:00Z")));

    mockMvc
        .perform(
            post("/api/performance/attribution")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "requestId": "REQ-ATTR-003",
                      "portfolioId": "PF-1001",
                      "valuationDate": "2026-06-14",
                      "requestedBy": "advisor01",
                      "groups": [
                        { "groupName": "Equity", "weightPct": 60, "returnPct": 2.5 },
                        { "groupName": "Fixed Income", "weightPct": 30 },
                        { "groupName": "Cash", "weightPct": 10, "returnPct": 0.1 }
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DEGRADED"))
        .andExpect(jsonPath("$.degraded").value(true));
  }
}
