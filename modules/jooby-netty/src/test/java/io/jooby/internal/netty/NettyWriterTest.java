/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NettyWriterTest {

  private ByteArrayOutputStream baos;
  private NettyWriter writer;

  @BeforeEach
  void setup() {
    baos = spy(new ByteArrayOutputStream());
    writer = new NettyWriter(baos, StandardCharsets.UTF_8);
  }

  @Test
  void testWriteCharArrayWithOffsetAndLength() throws IOException {
    char[] chars = {'A', 'B', 'C', 'D'};
    writer.write(chars, 1, 2);

    assertEquals("BC", baos.toString(StandardCharsets.UTF_8));
  }

  @Test
  void testWriteString() throws IOException {
    writer.write("Jooby");

    assertEquals("Jooby", baos.toString(StandardCharsets.UTF_8));
  }

  @Test
  void testWriteStringWithOffsetAndLength() throws IOException {
    // Standard substring check: length 3 starting at offset 1
    // "Hello".substring(1, 1 + 3) == "ell"
    writer.write("Hello", 1, 3);

    assertEquals("ell", baos.toString(StandardCharsets.UTF_8));
  }

  @Test
  void testWriteInt() throws IOException {
    // Testing with a non-ASCII character (ñ) to ensure the charset encoding
    // is properly applied (it requires 2 bytes in UTF-8).
    writer.write('ñ');

    assertEquals("ñ", baos.toString(StandardCharsets.UTF_8));
  }

  @Test
  void testWriteCharArray() throws IOException {
    char[] chars = {'X', 'Y', 'Z'};
    writer.write(chars);

    assertEquals("XYZ", baos.toString(StandardCharsets.UTF_8));
  }

  @Test
  void testAppendChar() throws IOException {
    // Testing with a non-ASCII character to verify charset encoding
    assertSame(writer, writer.append('ñ'));

    assertEquals("ñ", baos.toString(StandardCharsets.UTF_8));
  }

  @Test
  void testAppendCharSequence() throws IOException {
    assertSame(writer, writer.append("Sequence"));

    assertEquals("Sequence", baos.toString(StandardCharsets.UTF_8));
  }

  @Test
  void testAppendCharSequence_Null() {
    assertThrows(NullPointerException.class, () -> writer.append((CharSequence) null));
  }

  @Test
  void testAppendCharSequenceWithStartAndEnd() throws IOException {
    // "Sequence".subSequence(1, 4) == "equ"
    assertSame(writer, writer.append("Sequence", 1, 4));

    assertEquals("equ", baos.toString(StandardCharsets.UTF_8));
  }

  @Test
  void testAppendCharSequenceWithStartAndEnd_Null() {
    assertThrows(NullPointerException.class, () -> writer.append((CharSequence) null, 0, 1));
  }

  @Test
  void testFlush() throws IOException {
    writer.flush();
    verify(baos).flush();
  }

  @Test
  void testClose() throws IOException {
    writer.close();
    verify(baos).close();
  }
}
