package com.portfolio.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
class GatewayRouteConfigurationTest {

  @Autowired private RouteLocator routeLocator;

  @Test
  void portfolioPerformanceRoute_isConfigured() {
    StepVerifier.create(routeLocator.getRoutes())
        .assertNext(
            route -> {
              assertThat(route.getId()).isEqualTo("portfolio-performance-api");
              assertThat(route.getUri().toString()).contains("lb://portfolio-performance");
              assertThat(route.getPredicate().toString()).contains("/api/performance");
            })
        .verifyComplete();
  }
}
