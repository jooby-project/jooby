/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.jackson2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectReader;

class JacksonTrpcDecoderTest {

  private ObjectReader reader;
  private JacksonTrpcDecoder<Object> decoder;

  @BeforeEach
  void setUp() {
    reader = mock(ObjectReader.class);
    decoder = new JacksonTrpcDecoder<>(reader);
  }

  @Test
  void shouldDecodeByteArraySuccessfully() throws IOException {
    // Arrange
    byte[] payload = "{\"key\":\"value\"}".getBytes();
    Object expectedResult = new Object();
    when(reader.readValue(payload)).thenReturn(expectedResult);

    // Act
    Object actualResult = decoder.decode("someMethodName", payload);

    // Assert
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void shouldPropagateExceptionWhenDecodingByteArrayFails() throws IOException {
    // Arrange
    byte[] payload = "{\"key\":\"value\"}".getBytes();
    IOException expectedException = new IOException("Byte parsing error");
    when(reader.readValue(payload)).thenThrow(expectedException);

    // Act & Assert
    // SneakyThrows will propagate the original exception directly
    Exception thrown =
        assertThrows(Exception.class, () -> decoder.decode("someMethodName", payload));
    assertEquals(expectedException, thrown);
  }

  @Test
  void shouldDecodeStringSuccessfully() throws IOException {
    // Arrange
    String payload = "{\"key\":\"value\"}";
    Object expectedResult = new Object();
    when(reader.readValue(payload)).thenReturn(expectedResult);

    // Act
    Object actualResult = decoder.decode("someMethodName", payload);

    // Assert
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void shouldPropagateExceptionWhenDecodingStringFails() throws IOException {
    // Arrange
    String payload = "{\"key\":\"value\"}";
    RuntimeException expectedException = new RuntimeException("String parsing error");
    when(reader.readValue(payload)).thenThrow(expectedException);

    // Act & Assert
    // SneakyThrows will propagate the original exception directly
    Exception thrown =
        assertThrows(Exception.class, () -> decoder.decode("someMethodName", payload));
    assertEquals(expectedException, thrown);
  }
}
