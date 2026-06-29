package com.portfolio.performance.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ApplicationInfoContributor implements InfoContributor {

  private final Environment environment;

  public ApplicationInfoContributor(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void contribute(Info.Builder builder) {
    builder.withDetail("service", "portfolio-performance");
    builder.withDetail("profiles", environment.getActiveProfiles());
    builder.withDetail("javaVersion", System.getProperty("java.version"));
  }
}
