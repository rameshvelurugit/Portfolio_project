package com.portfolio.common.logging;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;

/**
 * Redacts sensitive HTTP headers before they are written to logs.
 */
public final class HeaderSanitizer {

  private HeaderSanitizer() {}

  public static Map<String, String> sanitize(HttpHeaders headers) {
    if (headers == null || headers.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, String> sanitized = new LinkedHashMap<>();
    headers.forEach(
        (name, values) -> {
          if (SensitiveHeaders.isSensitive(name)) {
            sanitized.put(name, "[REDACTED]");
          } else if (!values.isEmpty()) {
            sanitized.put(name, String.join(",", values));
          }
        });
    return sanitized;
  }

  public static Map<String, String> sanitize(Map<String, String> headers) {
    if (headers == null || headers.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, String> sanitized = new LinkedHashMap<>();
    headers.forEach(
        (name, value) ->
            sanitized.put(
                name, SensitiveHeaders.isSensitive(name) ? "[REDACTED]" : value));
    return sanitized;
  }
}
