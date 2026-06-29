package com.portfolio.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enriches MDC with handler metadata without emitting duplicate request logs.
 */
public class RequestLoggingInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (handler instanceof HandlerMethod handlerMethod) {
      String handlerName =
          handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
      MDC.put(LoggingMdcKeys.HANDLER, handlerName);
    }
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      Exception ex) {
    MDC.remove(LoggingMdcKeys.HANDLER);
  }
}
