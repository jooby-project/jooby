/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jte;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.jooby.output.OutputFactory;
import io.jooby.output.OutputOptions;

public class BufferedTemplateOutputTest {

  @Test
  public void checkWriteContent() {
    var factory = OutputFactory.create(OutputOptions.small());
    var buffer = factory.allocate();
    var output = new BufferedTemplateOutput(buffer, StandardCharsets.UTF_8);
    output.writeContent("Hello");
    assertEquals("Hello", StandardCharsets.UTF_8.decode(buffer.asByteBuffer()).toString());
  }

  @Test
  public void checkWriteContentSubstring() {
    var factory = OutputFactory.create(OutputOptions.small());
    var buffer = factory.allocate();
    var output = new BufferedTemplateOutput(buffer, StandardCharsets.UTF_8);
    output.writeContent(" Hello World! ", 1, " Hello World! ".length() - 2);
    assertEquals("Hello World", StandardCharsets.UTF_8.decode(buffer.asByteBuffer()).toString());
  }

  @Test
  public void checkWriteBinaryContent() {
    var factory = OutputFactory.create(OutputOptions.small());
    var buffer = factory.allocate();
    var output = new BufferedTemplateOutput(buffer, StandardCharsets.UTF_8);
    output.writeBinaryContent("Hello".getBytes(StandardCharsets.UTF_8));
    assertEquals("Hello", StandardCharsets.UTF_8.decode(buffer.asByteBuffer()).toString());
  }
}
