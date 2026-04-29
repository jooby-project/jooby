/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.MediaType;

public class OpenAPIAssetTest {

  @Test
  @DisplayName("Verify OpenAPIAsset properties, stream, and close behavior")
  void testOpenAPIAsset() throws Exception {
    // 1. Setup test data
    MediaType expectedType = MediaType.json;
    byte[] expectedContent = "{\"openapi\": \"3.0.0\"}".getBytes(StandardCharsets.UTF_8);
    long expectedLastModified = 1622505600000L; // Arbitrary epoch time

    // 2. Instantiate the static inner class
    OpenAPIAsset asset = new OpenAPIAsset(expectedType, expectedContent, expectedLastModified);

    // 3. Verify simple property accessors
    assertEquals(expectedContent.length, asset.getSize());
    assertEquals(expectedLastModified, asset.getLastModified());
    assertFalse(asset.isDirectory(), "Asset should never be treated as a directory");
    assertEquals(expectedType, asset.getContentType());

    // 4. Verify stream provides the exact byte content
    try (InputStream stream = asset.stream()) {
      byte[] actualContent = stream.readAllBytes();
      assertArrayEquals(
          expectedContent, actualContent, "Stream content should match the provided byte array");
    }

    // 5. Verify close() executes safely (NOOP)
    assertDoesNotThrow(asset::close, "Calling close() should not throw any exceptions");
  }
}
