package com.portfolio.performance.logging;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.portfolio.common.logging.SensitiveHeaders;
import org.junit.jupiter.api.Test;

class RequestLoggingUtilitiesTest {

  @Test
  void sensitiveHeaders_areIdentified() {
    assertTrue(SensitiveHeaders.isSensitive("authorization"));
    assertTrue(SensitiveHeaders.isSensitive("Cookie"));
  }
}
