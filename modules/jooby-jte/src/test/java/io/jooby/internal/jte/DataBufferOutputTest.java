/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jte;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.jooby.output.ByteBufferOutputFactory;

public class DataBufferOutputTest {

  @Test
  public void checkWriteContent() {
    var factory = new ByteBufferOutputFactory();
    var buffer = factory.newBufferedOutput();
    var output = new DataBufferOutput(buffer, StandardCharsets.UTF_8);
    output.writeContent("Hello");
    assertEquals("Hello", buffer.asString(StandardCharsets.UTF_8));
  }

  @Test
  public void checkWriteContentSubstring() {
    var factory = new ByteBufferOutputFactory();
    var buffer = factory.newBufferedOutput();
    var output = new DataBufferOutput(buffer, StandardCharsets.UTF_8);
    output.writeContent(" Hello World! ", 1, " Hello World! ".length() - 2);
    assertEquals("Hello World", buffer.asString(StandardCharsets.UTF_8));
  }

  @Test
  public void checkWriteBinaryContent() {
    var factory = new ByteBufferOutputFactory();
    var buffer = factory.newBufferedOutput();
    var output = new DataBufferOutput(buffer, StandardCharsets.UTF_8);
    output.writeBinaryContent("Hello".getBytes(StandardCharsets.UTF_8));
    assertEquals("Hello", buffer.asString(StandardCharsets.UTF_8));
  }
}
