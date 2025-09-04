/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.jooby.SneakyThrows;
import io.jooby.internal.output.CompositeOutput;
import io.jooby.internal.output.OutputStatic;

public class OutputTest {

  @Test
  public void bufferedOutput() {
    output(
        buffered -> {
          var buffer = ByteBuffer.wrap(". New Output API!!".getBytes(StandardCharsets.UTF_8));
          buffered.write("Hello".getBytes(StandardCharsets.UTF_8));
          buffered.write(" ");
          buffered.write("World!");
          assertEquals(
              "Hello World!", StandardCharsets.UTF_8.decode(buffered.asByteBuffer()).toString());
          assertEquals(12, buffered.size());
          assertEquals(
              "Hello World!", StandardCharsets.UTF_8.decode(buffered.asByteBuffer()).toString());
          buffered.write(buffer);
          assertEquals(
              "Hello World!. New Output API!!",
              StandardCharsets.UTF_8.decode(buffered.asByteBuffer()).toString());
          assertEquals(30, buffered.size());
        },
        new ByteBufferedOutput(false, 3));

    output(
        buffered -> {
          var buffer = ByteBuffer.wrap(". New Output API!!".getBytes(StandardCharsets.UTF_8));
          buffered.write(" Hello World! ".getBytes(StandardCharsets.UTF_8), 1, 12);
          assertEquals(
              "Hello World!", StandardCharsets.UTF_8.decode(buffered.asByteBuffer()).toString());
          assertEquals(12, buffered.size());
          assertEquals(
              "Hello World!", StandardCharsets.UTF_8.decode(buffered.asByteBuffer()).toString());
          buffered.write(buffer);
          assertEquals(
              "Hello World!. New Output API!!",
              StandardCharsets.UTF_8.decode(buffered.asByteBuffer()).toString());
          assertEquals(30, buffered.size());
        },
        new ByteBufferedOutput(false, 255));

    output(
        buffered -> {
          var bytes = "xxHello World!xx".getBytes(StandardCharsets.UTF_8);
          buffered.write(bytes, 2, bytes.length - 4);
          assertEquals(
              "Hello World!", StandardCharsets.UTF_8.decode(buffered.asByteBuffer()).toString());
          assertEquals(12, buffered.size());
        },
        new ByteBufferedOutput(false, 255));

    output(
        buffered -> {
          buffered.write((byte) 'A');
          assertEquals("A", StandardCharsets.UTF_8.decode(buffered.asByteBuffer()).toString());
          assertEquals(1, buffered.size());
        },
        new ByteBufferedOutput(false, 255));
  }

  private void output(SneakyThrows.Consumer<BufferedOutput> consumer, BufferedOutput... buffers) {
    Stream.of(buffers).forEach(consumer);
  }

  @Test
  public void chunkedOutput() {
    output(
        chunked -> {
          var buffer = ByteBuffer.allocateDirect(6);
          buffer.put("rld!".getBytes(StandardCharsets.UTF_8));
          buffer.flip();
          chunked.write((byte) 'H');
          assertEquals(1, chunked.size());
          chunked.write("ello".getBytes(StandardCharsets.UTF_8));
          assertEquals(5, chunked.size());
          chunked.write(" ");
          assertEquals(6, chunked.size());
          chunked.write(" Wor".getBytes(StandardCharsets.UTF_8), 1, 2);
          assertEquals(8, chunked.size());
          chunked.write(buffer);
          assertEquals(12, chunked.size());
          assertEquals(
              "Hello World!", StandardCharsets.UTF_8.decode(chunked.asByteBuffer()).toString());
          assertEquals(12, chunked.size());
        },
        new CompositeOutput());
  }

  @Test
  public void wrapOutput() throws IOException {
    var bytes = "xxHello World!xx".getBytes(StandardCharsets.UTF_8);
    var output = new OutputStatic(ByteBuffer.wrap(bytes, 2, bytes.length - 4));
    assertEquals("Hello World!", StandardCharsets.UTF_8.decode(output.asByteBuffer()).toString());
    assertEquals(12, output.size());
  }
}
