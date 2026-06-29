package com.portfolio.performance.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PerformanceControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void calculateDailyReturn_endToEnd_processesAssignmentSample() throws Exception {
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
        .andExpect(jsonPath("$.portfolioReturnPct").value(2.50))
        .andExpect(jsonPath("$.excessReturnPct").value(0.70))
        .andExpect(jsonPath("$.status").value("VALID"));
  }
}
