package com.portfolio.gateway.filter;

import com.portfolio.common.logging.HeaderSanitizer;
import com.portfolio.common.logging.LoggingMdcKeys;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Centralized gateway request logging without duplicating downstream service logs.
 */
@Component
public class GatewayRequestLoggingFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(GatewayRequestLoggingFilter.class);
  private static final String START_ATTRIBUTE = GatewayRequestLoggingFilter.class.getName() + ".start";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getURI().getPath();
    if (path.startsWith("/actuator")) {
      return chain.filter(exchange);
    }

    long start = System.currentTimeMillis();
    exchange.getAttributes().put(START_ATTRIBUTE, start);

  Map<String, String> headers = new LinkedHashMap<>();
    request
        .getHeaders()
        .forEach((name, values) -> headers.put(name, values.isEmpty() ? "" : String.join(",", values)));

    log.info(
        "Gateway request started method={} uri={} clientIp={} queryParams={} headers={} traceId={} spanId={}",
        request.getMethod(),
        path,
        resolveClientIp(request),
        request.getQueryParams(),
        HeaderSanitizer.sanitize(headers),
        MDC.get("traceId"),
        MDC.get("spanId"));

    return chain
        .filter(exchange)
        .doOnSuccess(
            ignored -> {
              long duration = System.currentTimeMillis() - start;
              int status = exchange.getResponse().getStatusCode() != null
                  ? exchange.getResponse().getStatusCode().value()
                  : 0;
              log.info(
                  "Gateway request completed method={} uri={} status={} durationMs={} traceId={} spanId={}",
                  request.getMethod(),
                  path,
                  status,
                  duration,
                  MDC.get("traceId"),
                  MDC.get("spanId"));
            })
        .doOnError(
            error -> {
              long duration = System.currentTimeMillis() - start;
              MDC.put(LoggingMdcKeys.EXCEPTION_CLASS, error.getClass().getName());
              MDC.put(LoggingMdcKeys.EXCEPTION_MESSAGE, error.getMessage());
              log.error(
                  "Gateway request failed method={} uri={} durationMs={} traceId={} spanId={}",
                  request.getMethod(),
                  path,
                  duration,
                  MDC.get("traceId"),
                  MDC.get("spanId"),
                  error);
            });
  }

  private String resolveClientIp(ServerHttpRequest request) {
    String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddress() != null
        ? request.getRemoteAddress().getAddress().getHostAddress()
        : "unknown";
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
