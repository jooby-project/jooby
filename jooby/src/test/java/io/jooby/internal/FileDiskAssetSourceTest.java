/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.handler.Asset;

public class FileDiskAssetSourceTest {

  @Test
  @DisplayName("Verify asset resolution and string representation")
  void testFileDiskAssetSource() {
    Path path = mock(Path.class);
    when(path.toString()).thenReturn("/var/www/index.html");

    FileDiskAssetSource source = new FileDiskAssetSource(path);

    // Verify resolve always returns an asset based on the initial filepath
    Asset asset = source.resolve("any/random/path");
    assertNotNull(asset);

    // Verify toString delegation
    assertEquals("/var/www/index.html", source.toString());
  }
}
