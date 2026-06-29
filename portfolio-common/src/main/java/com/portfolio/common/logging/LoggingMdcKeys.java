package com.portfolio.common.logging;

/**
 * MDC and JSON log field keys shared across services.
 */
public final class LoggingMdcKeys {

  public static final String APPLICATION_NAME = "applicationName";
  public static final String SERVICE_NAME = "serviceName";
  public static final String ENVIRONMENT = "environment";
  public static final String HTTP_METHOD = "httpMethod";
  public static final String REQUEST_URI = "requestUri";
  public static final String CLIENT_IP = "clientIp";
  public static final String RESPONSE_STATUS = "responseStatus";
  public static final String EXECUTION_TIME_MS = "executionTimeMs";
  public static final String USER_AGENT = "userAgent";
  public static final String EXCEPTION_CLASS = "exceptionClass";
  public static final String EXCEPTION_MESSAGE = "exceptionMessage";
  public static final String RESPONSE_SIZE = "responseSize";
  public static final String HANDLER = "handler";

  private LoggingMdcKeys() {}
}
