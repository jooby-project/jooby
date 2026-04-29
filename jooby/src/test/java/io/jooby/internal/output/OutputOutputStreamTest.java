/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.output.BufferedOutput;

public class OutputOutputStreamTest {

  private BufferedOutput bufferedOutput;
  private OutputOutputStream stream;

  @BeforeEach
  void setUp() {
    bufferedOutput = mock(BufferedOutput.class);
    stream = new OutputOutputStream(bufferedOutput);
  }

  @Test
  @DisplayName("Verify write(int) successfully writes a byte")
  void testWriteIntSuccess() throws IOException {
    stream.write(65); // ASCII 'A'

    verify(bufferedOutput).write((byte) 65);
  }

  @Test
  @DisplayName("Verify write(byte[], int, int) successfully writes array segments")
  void testWriteByteArraySuccess() throws IOException {
    byte[] data = {10, 20, 30, 40};

    stream.write(data, 1, 2);

    verify(bufferedOutput).write(data, 1, 2);
  }

  @Test
  @DisplayName("Verify write(byte[], int, int) does nothing if length is 0 or less")
  void testWriteByteArrayZeroLength() throws IOException {
    byte[] data = {10, 20, 30};

    stream.write(data, 0, 0);
    stream.write(data, 0, -1);

    verifyNoInteractions(bufferedOutput);
  }

  @Test
  @DisplayName("Verify close() is idempotent and safe to call multiple times")
  void testCloseMultipleTimes() {
    assertDoesNotThrow(
        () -> {
          stream.close();
          stream.close(); // Triggers the `if (this.closed) return;` branch
        });
  }

  @Test
  @DisplayName("Verify write(int) throws IOException when stream is closed")
  void testWriteIntWhenClosedThrows() throws IOException {
    stream.close();

    IOException ex = assertThrows(IOException.class, () -> stream.write(65));

    assertEquals("OutputStream is closed", ex.getMessage());
    verifyNoInteractions(bufferedOutput);
  }

  @Test
  @DisplayName("Verify write(byte[], int, int) throws IOException when stream is closed")
  void testWriteByteArrayWhenClosedThrows() throws IOException {
    stream.close();
    byte[] data = {1, 2, 3};

    IOException ex = assertThrows(IOException.class, () -> stream.write(data, 0, 3));

    assertEquals("OutputStream is closed", ex.getMessage());
    verifyNoInteractions(bufferedOutput);
  }
}
