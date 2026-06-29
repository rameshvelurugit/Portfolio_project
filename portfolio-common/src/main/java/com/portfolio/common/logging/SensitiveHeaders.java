package com.portfolio.common.logging;

import java.util.Locale;
import java.util.Set;

/**
 * Headers that must never appear in request logs.
 */
public final class SensitiveHeaders {

  private static final Set<String> SENSITIVE =
      Set.of(
          "authorization",
          "proxy-authorization",
          "cookie",
          "set-cookie",
          "x-api-key",
          "x-auth-token",
          "x-access-token");

  private SensitiveHeaders() {}

  public static boolean isSensitive(String headerName) {
    return headerName != null && SENSITIVE.contains(headerName.toLowerCase(Locale.ROOT));
  }

  public static Set<String> names() {
    return SENSITIVE;
  }
}
