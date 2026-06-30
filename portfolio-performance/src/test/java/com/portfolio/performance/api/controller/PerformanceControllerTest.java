package com.portfolio.performance.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.performance.api.dto.DailyReturnResponse;
import com.portfolio.performance.application.service.AttributionService;
import com.portfolio.performance.application.service.DailyReturnService;
import com.portfolio.performance.domain.CalculationStatus;
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
class PerformanceControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private DailyReturnService dailyReturnService;

  @MockBean private AttributionService attributionService;

  @Test
  void calculateDailyReturn_whenRequestIsValid_returnsOkWithResponseBody() throws Exception {
    when(dailyReturnService.calculateDailyReturn(any()))
        .thenReturn(
            new DailyReturnResponse(
                "PF-1001",
                LocalDate.of(2026, 6, 14),
                new BigDecimal("2.50"),
                new BigDecimal("1.80"),
                new BigDecimal("0.70"),
                CalculationStatus.VALID,
                List.of(),
                Instant.parse("2026-06-14T10:30:00Z")));

    mockMvc
        .perform(
            post("/api/performance/daily-return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "portfolioId": "PF-1001",
                      "valuationDate": "2026-06-14",
                      "beginMarketValue": 1000000,
                      "endMarketValue": 1035000,
                      "netCashFlow": 10000,
                      "benchmarkReturnPct": 1.8,
                      "currency": "USD",
                      "requestedBy": "advisor01"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.portfolioId").value("PF-1001"))
        .andExpect(jsonPath("$.portfolioReturnPct").value(2.50))
        .andExpect(jsonPath("$.status").value("VALID"))
        .andExpect(jsonPath("$.reasons").isEmpty());
  }

  @Test
  void calculateDailyReturn_whenRequiredFieldIsMissing_returnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/performance/daily-return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "valuationDate": "2026-06-14",
                      "beginMarketValue": 1000000,
                      "endMarketValue": 1035000,
                      "netCashFlow": 10000,
                      "benchmarkReturnPct": 1.8,
                      "currency": "USD",
                      "requestedBy": "advisor01"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("portfolioId"));
  }

  @Test
  void calculateDailyReturn_whenBusinessValidationFails_returnsOkWithInvalidInputStatus()
      throws Exception {
    when(dailyReturnService.calculateDailyReturn(any()))
        .thenReturn(
            new DailyReturnResponse(
                "PF-1001",
                LocalDate.of(2026, 6, 14),
                null,
                new BigDecimal("1.80"),
                null,
                CalculationStatus.INVALID_INPUT,
                List.of("beginMarketValue must be non-negative"),
                Instant.parse("2026-06-14T10:30:00Z")));

    mockMvc
        .perform(
            post("/api/performance/daily-return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "portfolioId": "PF-1001",
                      "valuationDate": "2026-06-14",
                      "beginMarketValue": -100,
                      "endMarketValue": 1035000,
                      "netCashFlow": 10000,
                      "benchmarkReturnPct": 1.8,
                      "currency": "USD",
                      "requestedBy": "advisor01"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.reasons[0]").value("beginMarketValue must be non-negative"));
  }

  @Test
  void calculateDailyReturn_whenReviewIsRequired_returnsOkWithReasons() throws Exception {
    when(dailyReturnService.calculateDailyReturn(any()))
        .thenReturn(
            new DailyReturnResponse(
                "PF-1001",
                LocalDate.of(2026, 6, 14),
                new BigDecimal("8.00"),
                new BigDecimal("1.80"),
                new BigDecimal("6.20"),
                CalculationStatus.REVIEW_REQUIRED,
                List.of("portfolio return deviates from benchmark by more than 5%"),
                Instant.parse("2026-06-14T10:30:00Z")));

    mockMvc
        .perform(
            post("/api/performance/daily-return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "portfolioId": "PF-1001",
                      "valuationDate": "2026-06-14",
                      "beginMarketValue": 1000000,
                      "endMarketValue": 1080000,
                      "netCashFlow": 0,
                      "benchmarkReturnPct": 1.8,
                      "currency": "USD",
                      "requestedBy": "advisor01"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"))
        .andExpect(
            jsonPath("$.reasons[0]")
                .value("portfolio return deviates from benchmark by more than 5%"));
  }
}
