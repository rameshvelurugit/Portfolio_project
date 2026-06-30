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
class PerformanceControllerAttributionIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void calculateAttribution_endToEnd_processesAssignmentSample() throws Exception {
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
        .andExpect(jsonPath("$.totalContributionPct").value(1.750))
        .andExpect(jsonPath("$.status").value("VALID"))
        .andExpect(jsonPath("$.degraded").value(false))
        .andExpect(jsonPath("$.groups[0].contributionPct").value(1.500))
        .andExpect(jsonPath("$.groups[1].contributionPct").value(0.240))
        .andExpect(jsonPath("$.groups[2].contributionPct").value(0.010));
  }

  @Test
  void calculateAttribution_whenDuplicateRequestId_returnsSameResponse() throws Exception {
    String body =
        """
        {
          "requestId": "REQ-ATTR-DUP",
          "portfolioId": "PF-1001",
          "valuationDate": "2026-06-14",
          "requestedBy": "advisor01",
          "groups": [
            { "groupName": "Equity", "weightPct": 60, "returnPct": 2.5 },
            { "groupName": "Fixed Income", "weightPct": 30, "returnPct": 0.8 },
            { "groupName": "Cash", "weightPct": 10, "returnPct": 0.1 }
          ]
        }
        """;

    mockMvc
        .perform(
            post("/api/performance/attribution")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalContributionPct").value(1.750));

    mockMvc
        .perform(
            post("/api/performance/attribution")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalContributionPct").value(1.750))
        .andExpect(jsonPath("$.status").value("VALID"));
  }
}
