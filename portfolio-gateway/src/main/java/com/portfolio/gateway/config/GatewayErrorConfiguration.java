package com.portfolio.gateway.config;

import java.util.Map;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Returns structured JSON errors from the gateway without exposing internal details.
 */
@Configuration
public class GatewayErrorConfiguration {

  @Bean
  @Order(-2)
  public ErrorWebExceptionHandler gatewayErrorWebExceptionHandler() {
    return (ServerWebExchange exchange, Throwable ex) -> {
      HttpStatus status = resolveStatus(ex);
      exchange.getResponse().setStatusCode(status);
      exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

      String message = status.is5xxServerError() ? "Gateway processing error" : ex.getMessage();
      String body =
          """
          {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}
          """
              .formatted(
                  java.time.Instant.now(),
                  status.value(),
                  status.getReasonPhrase(),
                  message,
                  exchange.getRequest().getURI().getPath());

      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes());
      return exchange.getResponse().writeWith(Mono.just(buffer));
    };
  }

  private HttpStatus resolveStatus(Throwable ex) {
    if (ex instanceof ResponseStatusException responseStatusException) {
      return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
    }
    return HttpStatus.BAD_GATEWAY;
  }
}
