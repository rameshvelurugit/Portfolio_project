package com.portfolio.performance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncTracingConfiguration {

  @Bean(name = "applicationTaskExecutor")
  public ThreadPoolTaskExecutor applicationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(8);
    executor.setThreadNamePrefix("portfolio-async-");
    executor.initialize();
    return executor;
  }

  @Bean
  public SimpleAsyncTaskExecutor simpleAsyncTaskExecutor() {
  SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("portfolio-simple-async-");
    return executor;
  }
}
