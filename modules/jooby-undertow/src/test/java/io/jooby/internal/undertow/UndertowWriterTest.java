/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UndertowWriterTest {

  @Mock OutputStream out;

  private Charset charset;
  private UndertowWriter writer;

  @BeforeEach
  void setup() {
    charset = StandardCharsets.UTF_8;
    writer = new UndertowWriter(out, charset);
  }

  @Test
  void testWriteCharArrayOffsetLength() throws IOException {
    char[] chars = "Hello World".toCharArray();
    // Offset 6, Length 5 -> "World"
    writer.write(chars, 6, 5);

    byte[] expected = "World".getBytes(charset);
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

    verify(out).write(captor.capture(), eq(0), eq(expected.length));
    assertArrayEquals(expected, captor.getValue());
  }

  @Test
  void testWriteString() throws IOException {
    writer.write("Jooby");

    byte[] expected = "Jooby".getBytes(charset);
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

    verify(out).write(captor.capture(), eq(0), eq(expected.length));
    assertArrayEquals(expected, captor.getValue());
  }

  @Test
  void testWriteStringOffsetLength() throws IOException {
    // Offset 1, Length 5 -> Should extract "ramew", NOT "rame"
    writer.write("Framework", 1, 5);

    byte[] expected = "ramew".getBytes(charset);
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

    verify(out).write(captor.capture(), eq(0), eq(expected.length));
    assertArrayEquals(expected, captor.getValue());
  }

  @Test
  void testWriteInt() throws IOException {
    writer.write('X');

    // out.write(int) is invoked
    verify(out).write('X');
  }

  @Test
  void testWriteCharArray() throws IOException {
    char[] chars = "Jooby".toCharArray();
    writer.write(chars);

    byte[] expected = "Jooby".getBytes(charset);
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

    verify(out).write(captor.capture(), eq(0), eq(expected.length));
    assertArrayEquals(expected, captor.getValue());
  }

  @Test
  void testAppendChar() throws IOException {
    Writer result = writer.append('Z');

    assertSame(writer, result);
    verify(out).write('Z');
  }

  @Test
  void testAppendCharSequence() throws IOException {
    CharSequence seq = new StringBuilder("Builder");
    Writer result = writer.append(seq);

    assertSame(writer, result);

    byte[] expected = "Builder".getBytes(charset);
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

    verify(out).write(captor.capture(), eq(0), eq(expected.length));
    assertArrayEquals(expected, captor.getValue());
  }

  @Test
  void testAppendCharSequenceThrowsNPEOnNull() {
    assertThrows(NullPointerException.class, () -> writer.append(null));
  }

  @Test
  void testAppendCharSequenceOffsetLength() throws IOException {
    CharSequence seq = new StringBuilder("Undertow");
    // subSequence(5, 8) -> "tow"
    Writer result = writer.append(seq, 5, 8);

    assertSame(writer, result);

    byte[] expected = "tow".getBytes(charset);
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

    verify(out).write(captor.capture(), eq(0), eq(expected.length));
    assertArrayEquals(expected, captor.getValue());
  }

  @Test
  void testAppendCharSequenceOffsetLengthThrowsNPEOnNull() {
    assertThrows(NullPointerException.class, () -> writer.append(null, 0, 1));
  }

  @Test
  void testFlush() throws IOException {
    writer.flush();
    verify(out).flush();
  }

  @Test
  void testClose() throws IOException {
    writer.close();
    verify(out).close();
  }
}
