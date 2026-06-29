package com.portfolio.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class HeaderSanitizerTest {

  @Test
  void sanitize_redactsSensitiveHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer secret");
    headers.add("X-Request-Id", "abc-123");

    Map<String, String> sanitized = HeaderSanitizer.sanitize(headers);

    assertEquals("[REDACTED]", sanitized.get("Authorization"));
    assertEquals("abc-123", sanitized.get("X-Request-Id"));
  }

  @Test
  void isSensitive_matchesKnownHeaders() {
    assertTrue(SensitiveHeaders.isSensitive("Authorization"));
    assertFalse(SensitiveHeaders.isSensitive("Content-Type"));
  }
}
