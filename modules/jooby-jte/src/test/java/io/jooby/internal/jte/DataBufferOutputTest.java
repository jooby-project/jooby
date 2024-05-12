/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jte;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.jooby.buffer.DefaultDataBufferFactory;

public class DataBufferOutputTest {

  @Test
  public void checkWriteContent() {
    var factory = new DefaultDataBufferFactory();
    var buffer = factory.allocateBuffer();
    var output = new DataBufferOutput(buffer, StandardCharsets.UTF_8);
    output.writeContent("Hello");
    assertEquals("Hello", buffer.toString(StandardCharsets.UTF_8));
  }

  @Test
  public void checkWriteContentSubstring() {
    var factory = new DefaultDataBufferFactory();
    var buffer = factory.allocateBuffer();
    var output = new DataBufferOutput(buffer, StandardCharsets.UTF_8);
    output.writeContent(" Hello World! ", 1, " Hello World! ".length() - 2);
    assertEquals("Hello World", buffer.toString(StandardCharsets.UTF_8));
  }

  @Test
  public void checkWriteBinaryContent() {
    var factory = new DefaultDataBufferFactory();
    var buffer = factory.allocateBuffer();
    var output = new DataBufferOutput(buffer, StandardCharsets.UTF_8);
    output.writeBinaryContent("Hello".getBytes(StandardCharsets.UTF_8));
    assertEquals("Hello", buffer.toString(StandardCharsets.UTF_8));
  }
}
