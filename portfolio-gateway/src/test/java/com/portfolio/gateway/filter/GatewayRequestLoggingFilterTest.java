package com.portfolio.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.common.logging.SensitiveHeaders;
import org.junit.jupiter.api.Test;

class GatewayRequestLoggingFilterTest {

  @Test
  void sensitiveHeaders_areRedactedFromLogs() {
    assertThat(SensitiveHeaders.isSensitive("Authorization")).isTrue();
    assertThat(SensitiveHeaders.isSensitive("Content-Type")).isFalse();
  }
}
