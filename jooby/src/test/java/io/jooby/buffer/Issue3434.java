/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.jooby.SneakyThrows;

public class Issue3434 {
  DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

  @Test
  void shouldWriteCharBufferOnBufferWriter() throws IOException {
    /* CharArray */
    assertEquals(
        "Hello World",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World".toCharArray());
            }));
    assertEquals(
        "Hello World",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World".toCharArray(), 0, "Hello World".length());
            }));
    assertEquals(
        "ello World",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World".toCharArray(), 1, "ello World".length());
            }));

    assertEquals(
        "llo World",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World".toCharArray(), 2, "llo World".length());
            }));
    assertEquals(
        "ello Worl",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World".toCharArray(), 1, "ello Worl".length());
            }));
    assertEquals(
        "Hello Worl",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World".toCharArray(), 0, "Hello World".length() - 1);
            }));
    assertEquals(
        "Hello Wor",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World".toCharArray(), 0, "Hello World".length() - 2);
            }));
    assertEquals(
        "Hello Wo",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World".toCharArray(), 0, "Hello World".length() - 3);
            }));

    /* String */
    assertEquals(
        "Hello World",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World");
            }));
    assertEquals(
        "Hello World",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World", 0, "Hello World".length());
            }));
    assertEquals(
        "ello World",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World", 1, "ello World".length());
            }));

    assertEquals(
        "llo World",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World", 2, "llo World".length());
            }));
    assertEquals(
        "ello Worl",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World", 1, "ello Worl".length());
            }));
    assertEquals(
        "Hello Worl",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World", 0, "Hello World".length() - 1);
            }));
    assertEquals(
        "Hello Wor",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World", 0, "Hello World".length() - 2);
            }));
    assertEquals(
        "Hello Wo",
        writeCharSequence(
            StandardCharsets.UTF_8,
            writer -> {
              writer.write("Hello World", 0, "Hello World".length() - 3);
            }));
  }

  private String writeCharSequence(Charset charset, SneakyThrows.Consumer<Writer> writer)
      throws IOException {
    var buffer = factory.allocateBuffer();
    try (var out = buffer.asWriter(charset)) {
      writer.accept(out);
      return buffer.toString(charset);
    }
  }
}
