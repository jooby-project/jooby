/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BufferedOutputTest {

  private BufferedOutput output;

  @BeforeEach
  void setUp() {
    // CALLS_REAL_METHODS tells Mockito to execute the actual 'default' interface implementations
    // instead of returning null for those methods.
    output = mock(BufferedOutput.class, CALLS_REAL_METHODS);
  }

  @Test
  @DisplayName("Verify asOutputStream returns an OutputOutputStream instance")
  void testAsOutputStream() {
    OutputStream os = output.asOutputStream();
    assertNotNull(os);
    assertEquals("OutputOutputStream", os.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify asWriter defaults to an OutputWriter using UTF-8")
  void testAsWriterDefault() {
    Writer writer = output.asWriter();
    assertNotNull(writer);
    assertEquals("OutputWriter", writer.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify asWriter applies custom Charsets correctly")
  void testAsWriterCustomCharset() {
    Writer writer = output.asWriter(StandardCharsets.UTF_16);
    assertNotNull(writer);
    assertEquals("OutputWriter", writer.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify write(String) routes to UTF-8 and delegates to write(byte[])")
  void testWriteStringDefault() {
    output.write("hello");
    // It should automatically encode to UTF-8 and call the byte[] signature
    verify(output).write("hello".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("Verify write(String, Charset) fast-exits and returns 'this' for empty strings")
  void testWriteStringEmpty() {
    BufferedOutput result = output.write("", StandardCharsets.UTF_8);

    assertSame(output, result);
    verify(output, never()).write(any(byte[].class)); // Ensures it never attempted to write bytes
  }

  @Test
  @DisplayName("Verify write(String, Charset) processes non-empty strings into write(byte[])")
  void testWriteStringWithCharset() {
    output.write("test", StandardCharsets.UTF_16);
    verify(output).write("test".getBytes(StandardCharsets.UTF_16));
  }

  @Test
  @DisplayName(
      "Verify write(ByteBuffer) utilizes the fast-path offset/length array write if possible")
  void testWriteByteBufferWithArray() {
    byte[] data = {10, 20, 30, 40};
    ByteBuffer buffer = ByteBuffer.wrap(data);

    // Move the position to simulate a partial read: offset 1, remaining 2
    buffer.position(1);
    buffer.limit(3);

    output.write(buffer);

    // Verify it extracted the backing array and used the offset/length signature
    verify(output).write(data, 1, 2);
  }

  @Test
  @DisplayName(
      "Verify write(ByteBuffer) extracts bytes to a new array if it lacks a backing array (e.g."
          + " DirectBuffer)")
  void testWriteByteBufferDirect() {
    // DirectByteBuffers do not have accessible backing arrays (hasArray() == false)
    ByteBuffer directBuffer = ByteBuffer.allocateDirect(3);
    directBuffer.put(new byte[] {5, 6, 7});
    directBuffer.flip();

    output.write(directBuffer);

    // Verify it manually created a new byte[] from the buffer and delegated to write(byte[])
    verify(output).write(new byte[] {5, 6, 7});
  }

  @Test
  @DisplayName("Verify write(CharBuffer, Charset) fast-exits and returns 'this' for empty buffers")
  void testWriteCharBufferEmpty() {
    CharBuffer empty = CharBuffer.allocate(0);
    BufferedOutput result = output.write(empty, StandardCharsets.UTF_8);

    assertSame(output, result);
    verify(output, never()).write(any(ByteBuffer.class));
  }

  @Test
  @DisplayName(
      "Verify write(CharBuffer, Charset) encodes characters and delegates to write(ByteBuffer)")
  void testWriteCharBufferWithContent() {
    CharBuffer chars = CharBuffer.wrap("test data");
    output.write(chars, StandardCharsets.UTF_8);

    // After encoding, it should delegate to the ByteBuffer signature
    verify(output).write(any(ByteBuffer.class));
  }
}
