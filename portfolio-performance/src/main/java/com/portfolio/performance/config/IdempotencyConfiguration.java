package com.portfolio.performance.config;

import com.portfolio.performance.application.idempotency.IdempotencyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyConfiguration {}
