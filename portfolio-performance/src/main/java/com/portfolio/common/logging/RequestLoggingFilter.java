package com.portfolio.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Centralized servlet request logging. Emits a single start and completion log per request.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
  private static final String START_ATTRIBUTE = RequestLoggingFilter.class.getName() + ".startTime";

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long start = System.currentTimeMillis();
    request.setAttribute(START_ATTRIBUTE, start);

    putRequestMdc(request);
    log.info(
        "Request started method={} uri={} clientIp={} queryParams={} headers={} traceId={} spanId={}",
        request.getMethod(),
        request.getRequestURI(),
        RequestLogSupport.resolveClientIp(request),
        RequestLogSupport.queryParameters(request),
        RequestLogSupport.requestHeaders(request),
        MDC.get("traceId"),
        MDC.get("spanId"));

    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
    try {
      filterChain.doFilter(request, wrappedResponse);
      logCompletion(request, wrappedResponse, null);
      wrappedResponse.copyBodyToResponse();
    } catch (Exception ex) {
      logCompletion(request, wrappedResponse, ex);
      wrappedResponse.copyBodyToResponse();
      throw ex;
    } finally {
      clearRequestMdc();
    }
  }

  private void logCompletion(
      HttpServletRequest request, ContentCachingResponseWrapper response, Exception exception) {
    long start = (long) request.getAttribute(START_ATTRIBUTE);
    long duration = System.currentTimeMillis() - start;
    int status = response.getStatus();
    int responseSize = response.getContentAsByteArray().length;

    MDC.put(LoggingMdcKeys.RESPONSE_STATUS, String.valueOf(status));
    MDC.put(LoggingMdcKeys.EXECUTION_TIME_MS, String.valueOf(duration));
    MDC.put(LoggingMdcKeys.RESPONSE_SIZE, String.valueOf(responseSize));

    if (exception != null) {
      MDC.put(LoggingMdcKeys.EXCEPTION_CLASS, exception.getClass().getName());
      MDC.put(LoggingMdcKeys.EXCEPTION_MESSAGE, exception.getMessage());
      log.error(
          "Request failed method={} uri={} status={} durationMs={} responseSize={} traceId={} spanId={}",
          request.getMethod(),
          request.getRequestURI(),
          status,
          duration,
          responseSize,
          MDC.get("traceId"),
          MDC.get("spanId"),
          exception);
    } else {
      log.info(
          "Request completed method={} uri={} status={} durationMs={} responseSize={} traceId={} spanId={}",
          request.getMethod(),
          request.getRequestURI(),
          status,
          duration,
          responseSize,
          MDC.get("traceId"),
          MDC.get("spanId"));
    }
  }

  private void putRequestMdc(HttpServletRequest request) {
    MDC.put(LoggingMdcKeys.HTTP_METHOD, request.getMethod());
    MDC.put(LoggingMdcKeys.REQUEST_URI, request.getRequestURI());
    MDC.put(LoggingMdcKeys.CLIENT_IP, RequestLogSupport.resolveClientIp(request));
    String userAgent = request.getHeader("User-Agent");
    if (userAgent != null) {
      MDC.put(LoggingMdcKeys.USER_AGENT, userAgent);
    }
  }

  private void clearRequestMdc() {
    Map<String, String> copy = MDC.getCopyOfContextMap();
    if (copy == null) {
      return;
    }
    copy.keySet().stream()
        .filter(
            key ->
                key.equals(LoggingMdcKeys.HTTP_METHOD)
                    || key.equals(LoggingMdcKeys.REQUEST_URI)
                    || key.equals(LoggingMdcKeys.CLIENT_IP)
                    || key.equals(LoggingMdcKeys.USER_AGENT)
                    || key.equals(LoggingMdcKeys.RESPONSE_STATUS)
                    || key.equals(LoggingMdcKeys.EXECUTION_TIME_MS)
                    || key.equals(LoggingMdcKeys.RESPONSE_SIZE)
                    || key.equals(LoggingMdcKeys.EXCEPTION_CLASS)
                    || key.equals(LoggingMdcKeys.EXCEPTION_MESSAGE)
                    || key.equals(LoggingMdcKeys.HANDLER))
        .forEach(MDC::remove);
  }
}
