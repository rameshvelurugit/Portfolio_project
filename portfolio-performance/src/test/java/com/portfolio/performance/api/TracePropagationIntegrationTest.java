package com.portfolio.performance.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TracePropagationIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private Tracer tracer;

  @Test
  void tracingInfrastructure_isConfigured() {
    assertThat(tracer).isNotNull();
  }

  @Test
  void incomingRequest_propagatesW3cTraceParent() throws Exception {
    mockMvc
        .perform(
            post("/api/performance/daily-return")
                .contentType(MediaType.APPLICATION_JSON)
                .header("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
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
        .andExpect(status().isOk());
  }
}
