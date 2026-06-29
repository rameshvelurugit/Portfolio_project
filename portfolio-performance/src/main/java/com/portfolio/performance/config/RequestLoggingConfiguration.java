package com.portfolio.performance.config;

import com.portfolio.common.logging.RequestLoggingFilter;
import com.portfolio.common.logging.RequestLoggingInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RequestLoggingConfiguration implements WebMvcConfigurer {

  @Bean
  public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
    FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new RequestLoggingFilter());
    registration.setOrder(Integer.MIN_VALUE + 10);
    return registration;
  }

  @Bean
  public RequestLoggingInterceptor requestLoggingInterceptor() {
    return new RequestLoggingInterceptor();
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(requestLoggingInterceptor());
  }
}
