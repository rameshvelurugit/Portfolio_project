package com.portfolio.performance.application.idempotency;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for distributed idempotency storage.
 */
@ConfigurationProperties(prefix = "portfolio.idempotency")
public class IdempotencyProperties {

  /** Storage backend: {@code redis} or {@code memory}. */
  private String store = "redis";

  /** How long completed responses remain in the cache. */
  private Duration ttl = Duration.ofHours(24);

  /** How long an in-flight processing lock is held. */
  private Duration lockTtl = Duration.ofSeconds(30);

  /** Maximum time to wait for another instance to finish processing the same request id. */
  private Duration waitTimeout = Duration.ofSeconds(5);

  /** Redis key prefix for attribution idempotency entries. */
  private String keyPrefix = "portfolio:idempotency:attribution";

  public String getStore() {
    return store;
  }

  public void setStore(String store) {
    this.store = store;
  }

  public Duration getTtl() {
    return ttl;
  }

  public void setTtl(Duration ttl) {
    this.ttl = ttl;
  }

  public Duration getLockTtl() {
    return lockTtl;
  }

  public void setLockTtl(Duration lockTtl) {
    this.lockTtl = lockTtl;
  }

  public Duration getWaitTimeout() {
    return waitTimeout;
  }

  public void setWaitTimeout(Duration waitTimeout) {
    this.waitTimeout = waitTimeout;
  }

  public String getKeyPrefix() {
    return keyPrefix;
  }

  public void setKeyPrefix(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }
}
