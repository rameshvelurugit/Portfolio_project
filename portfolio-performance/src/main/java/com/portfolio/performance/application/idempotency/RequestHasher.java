package com.portfolio.performance.application.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.performance.api.dto.AttributionRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Produces a stable hash of an attribution request for duplicate-body detection.
 */
@Component
public class RequestHasher {

  private final ObjectMapper objectMapper;

  public RequestHasher(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Returns a SHA-256 hex digest of the canonical JSON representation of the request.
   */
  public String hash(AttributionRequest request) {
    try {
      byte[] json = objectMapper.writeValueAsBytes(request);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(json));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize attribution request for hashing", exception);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 not available", exception);
    }
  }
}
