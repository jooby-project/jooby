/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jooby.output.BufferedOutput;

public class OutputWriterTest {

  private BufferedOutput bufferedOutput;
  private OutputWriter writer;

  @BeforeEach
  void setUp() {
    bufferedOutput = mock(BufferedOutput.class);
    writer = new OutputWriter(bufferedOutput, StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("Verify write(int) converts the int to a char and wraps it in a CharBuffer")
  void testWriteInt() throws IOException {
    writer.write(65); // ASCII 'A'

    ArgumentCaptor<CharBuffer> captor = ArgumentCaptor.forClass(CharBuffer.class);
    verify(bufferedOutput).write(captor.capture(), eq(StandardCharsets.UTF_8));

    assertEquals("A", captor.getValue().toString());
  }

  @Test
  @DisplayName("Verify write(char[]) delegates to write(char[], offset, length)")
  void testWriteCharArray() throws IOException {
    char[] data = {'H', 'i'};
    writer.write(data);

    ArgumentCaptor<CharBuffer> captor = ArgumentCaptor.forClass(CharBuffer.class);
    verify(bufferedOutput).write(captor.capture(), eq(StandardCharsets.UTF_8));

    assertEquals("Hi", captor.getValue().toString());
  }

  @Test
  @DisplayName("Verify write(char[], offset, length) wraps the sliced array in a CharBuffer")
  void testWriteCharArrayWithOffset() throws IOException {
    char[] data = {'a', 'b', 'c', 'd'};
    writer.write(data, 1, 2); // Should extract 'b', 'c'

    ArgumentCaptor<CharBuffer> captor = ArgumentCaptor.forClass(CharBuffer.class);
    verify(bufferedOutput).write(captor.capture(), eq(StandardCharsets.UTF_8));

    assertEquals("bc", captor.getValue().toString());
  }

  @Test
  @DisplayName("Verify write(String) passes the raw string and charset down to the output")
  void testWriteString() throws IOException {
    writer.write("Jooby");

    verify(bufferedOutput).write("Jooby", StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("Verify write(String, offset, length) wraps the sliced string in a CharBuffer")
  void testWriteStringWithOffset() throws IOException {
    writer.write("Hello World", 6, 5); // Should extract "World"

    ArgumentCaptor<CharBuffer> captor = ArgumentCaptor.forClass(CharBuffer.class);
    verify(bufferedOutput).write(captor.capture(), eq(StandardCharsets.UTF_8));

    assertEquals("World", captor.getValue().toString());
  }

  @Test
  @DisplayName("Verify flush() executes safely without error")
  void testFlush() {
    assertDoesNotThrow(() -> writer.flush());
  }

  @Test
  @DisplayName("Verify close() is idempotent and safely ignores subsequent calls")
  void testCloseMultipleTimes() {
    assertDoesNotThrow(
        () -> {
          writer.close();
          writer.close(); // Triggers the `if (this.closed) return;` branch
        });
  }

  @Test
  @DisplayName("Verify checkClosed throws IOException for write(int) when closed")
  void testWriteIntWhenClosedThrows() throws IOException {
    writer.close();

    IOException ex = assertThrows(IOException.class, () -> writer.write(65));
    assertEquals("Writer is closed", ex.getMessage());
    verifyNoInteractions(bufferedOutput);
  }

  @Test
  @DisplayName("Verify checkClosed throws IOException for write(char[], off, len) when closed")
  void testWriteCharArrayWhenClosedThrows() throws IOException {
    writer.close();

    IOException ex = assertThrows(IOException.class, () -> writer.write(new char[] {'a'}, 0, 1));
    assertEquals("Writer is closed", ex.getMessage());
    verifyNoInteractions(bufferedOutput);
  }

  @Test
  @DisplayName("Verify checkClosed throws IOException for write(String) when closed")
  void testWriteStringWhenClosedThrows() throws IOException {
    writer.close();

    IOException ex = assertThrows(IOException.class, () -> writer.write("test"));
    assertEquals("Writer is closed", ex.getMessage());
    verifyNoInteractions(bufferedOutput);
  }

  @Test
  @DisplayName("Verify checkClosed throws IOException for write(String, off, len) when closed")
  void testWriteStringWithOffsetWhenClosedThrows() throws IOException {
    writer.close();

    IOException ex = assertThrows(IOException.class, () -> writer.write("test", 0, 4));
    assertEquals("Writer is closed", ex.getMessage());
    verifyNoInteractions(bufferedOutput);
  }
}
