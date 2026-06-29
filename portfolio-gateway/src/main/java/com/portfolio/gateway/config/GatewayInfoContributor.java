package com.portfolio.gateway.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class GatewayInfoContributor implements InfoContributor {

  private final Environment environment;

  public GatewayInfoContributor(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void contribute(Info.Builder builder) {
    builder.withDetail("service", "portfolio-gateway");
    builder.withDetail("profiles", environment.getActiveProfiles());
    builder.withDetail("javaVersion", System.getProperty("java.version"));
  }
}
