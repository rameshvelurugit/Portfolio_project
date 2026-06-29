package com.portfolio.performance.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide beans that support testability.
 */
@Configuration
public class ClockConfig {

  /**
   * Provides a {@link Clock} so services can use a fixed clock in tests.
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
