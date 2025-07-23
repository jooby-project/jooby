/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class ByteBufferedOutputTest {

  @Test
  public void shouldReadMultipleTimes() {
    var factory = OutputFactory.create(new OutputOptions().setSize(4).setDirectBuffers(false));
    var output = factory.allocate();
    output.write("hello");
    output.write((byte) 32);
    output.write("world");
    assertEquals("hello world", StandardCharsets.UTF_8.decode(output.asByteBuffer()).toString());
    assertEquals("hello world", StandardCharsets.UTF_8.decode(output.asByteBuffer()).toString());
  }

  @Test
  public void shouldCheckReadOnlyBuffer() {
    var factory = OutputFactory.create(new OutputOptions().setSize(4).setDirectBuffers(false));
    var output = factory.allocate();
    output.write("hello");
    assertThrows(
        ReadOnlyBufferException.class,
        () -> output.asByteBuffer().put("world".getBytes(StandardCharsets.UTF_8)));
    assertEquals("hello", StandardCharsets.UTF_8.decode(output.asByteBuffer()).toString());
  }
}
