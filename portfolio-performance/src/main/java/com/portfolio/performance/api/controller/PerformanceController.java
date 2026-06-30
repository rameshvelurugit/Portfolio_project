package com.portfolio.performance.api.controller;

import com.portfolio.performance.api.dto.AttributionRequest;
import com.portfolio.performance.api.dto.AttributionResponse;
import com.portfolio.performance.api.dto.DailyReturnRequest;
import com.portfolio.performance.api.dto.DailyReturnResponse;
import com.portfolio.performance.application.service.AttributionService;
import com.portfolio.performance.application.service.DailyReturnService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for portfolio performance calculations.
 */
@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

  private final DailyReturnService dailyReturnService;
  private final AttributionService attributionService;

  public PerformanceController(
      DailyReturnService dailyReturnService, AttributionService attributionService) {
    this.dailyReturnService = dailyReturnService;
    this.attributionService = attributionService;
  }

  /**
   * Calculates the daily portfolio return for a single valuation date.
   *
   * @param request portfolio values, cash flow, and benchmark data
   * @return calculated returns with a business status and any review reasons
   */
  @PostMapping("/daily-return")
  public ResponseEntity<DailyReturnResponse> calculateDailyReturn(
      @Valid @RequestBody DailyReturnRequest request) {
    DailyReturnResponse response = dailyReturnService.calculateDailyReturn(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Calculates how each asset group contributes to overall portfolio return.
   *
   * @param request portfolio groups with weights and returns
   * @return per-group contributions with a business status and any warnings
   */
  @PostMapping("/attribution")
  public ResponseEntity<AttributionResponse> calculateAttribution(
      @Valid @RequestBody AttributionRequest request) {
    AttributionResponse response = attributionService.calculateAttribution(request);
    return ResponseEntity.ok(response);
  }
}
