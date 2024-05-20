/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class LoggingServiceTest {

  @Test
  public void isBinaryPath() {
    assertFalse(LoggingService.isBinary(Paths.get("src", "main", "java")));
    assertTrue(LoggingService.isBinary(Paths.get("target")));
    assertTrue(LoggingService.isBinary(Paths.get("project", "target")));
    assertTrue(LoggingService.isBinary(Paths.get("project", "build")));
    assertTrue(LoggingService.isBinary(Paths.get("project", "bin")));
  }
}
