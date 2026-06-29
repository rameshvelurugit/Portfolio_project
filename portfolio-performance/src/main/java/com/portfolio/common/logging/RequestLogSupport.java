package com.portfolio.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utilities for enriching structured logs with request context.
 */
public final class RequestLogSupport {

  private RequestLogSupport() {}

  public static String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  public static Map<String, String> requestHeaders(HttpServletRequest request) {
    Map<String, String> headers = new LinkedHashMap<>();
    Enumeration<String> names = request.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      if (SensitiveHeaders.isSensitive(name)) {
        headers.put(name, "[REDACTED]");
      } else {
        headers.put(name, Collections.list(request.getHeaders(name)).stream().collect(Collectors.joining(",")));
      }
    }
    return headers;
  }

  public static Map<String, String> queryParameters(HttpServletRequest request) {
    if (request.getParameterMap().isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, String> params = new LinkedHashMap<>();
    request
        .getParameterMap()
        .forEach((key, values) -> params.put(key, String.join(",", values)));
    return params;
  }
}
