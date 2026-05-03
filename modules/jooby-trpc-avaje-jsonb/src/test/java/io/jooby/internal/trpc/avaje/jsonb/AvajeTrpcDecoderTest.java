/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.avaje.jsonb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.avaje.jsonb.JsonType;

class AvajeTrpcDecoderTest {

  private JsonType<Object> typeAdapter;
  private AvajeTrpcDecoder<Object> decoder;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    // Mock the Avaje JsonType adapter
    typeAdapter = mock(JsonType.class);
    decoder = new AvajeTrpcDecoder<>(typeAdapter);
  }

  @Test
  void shouldDecodeByteArrayPayload() {
    // Arrange
    byte[] payload = "{\"key\":\"value\"}".getBytes();
    Object expectedObject = new Object();

    when(typeAdapter.fromJson(payload)).thenReturn(expectedObject);

    // Act
    Object result = decoder.decode("paramName", payload);

    // Assert
    assertEquals(expectedObject, result);
    verify(typeAdapter).fromJson(payload);
  }

  @Test
  void shouldDecodeStringPayload() {
    // Arrange
    String payload = "{\"key\":\"value\"}";
    Object expectedObject = new Object();

    when(typeAdapter.fromJson(payload)).thenReturn(expectedObject);

    // Act
    Object result = decoder.decode("paramName", payload);

    // Assert
    assertEquals(expectedObject, result);
    verify(typeAdapter).fromJson(payload);
  }
}
