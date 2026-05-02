/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.jackson2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Type;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.jooby.trpc.TrpcDecoder;
import io.jooby.trpc.TrpcReader;

class JacksonTrpcParserTest {

  private ObjectMapper mapper;
  private JacksonTrpcParser parser;

  @BeforeEach
  void setUp() {
    mapper = mock(ObjectMapper.class);
    parser = new JacksonTrpcParser(mapper);
  }

  @Test
  void shouldCreateDecoder() {
    // Arrange
    Type type = String.class;
    JavaType javaType = mock(JavaType.class);
    ObjectReader objectReader = mock(ObjectReader.class);
    ObjectReader objectReaderWithoutFeature = mock(ObjectReader.class);

    when(mapper.constructType(type)).thenReturn(javaType);
    when(mapper.readerFor(javaType)).thenReturn(objectReader);
    when(objectReader.without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS))
        .thenReturn(objectReaderWithoutFeature);

    // Act
    TrpcDecoder<Object> decoder = parser.decoder(type);

    // Assert
    assertNotNull(decoder);
    assertTrue(decoder instanceof JacksonTrpcDecoder);
    verify(mapper).constructType(type);
    verify(mapper).readerFor(javaType);
    verify(objectReader).without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
  }

  @Test
  void shouldCreateReaderFromByteArray() throws IOException {
    // Arrange
    byte[] payload = "{\"test\": 1}".getBytes();
    boolean isTuple = false; // False avoids the tuple array validation check
    JsonParser jsonParser = mock(JsonParser.class);

    when(mapper.createParser(payload)).thenReturn(jsonParser);

    // Act
    TrpcReader reader = parser.reader(payload, isTuple);

    // Assert
    assertNotNull(reader);
    assertTrue(reader instanceof JacksonTrpcReader);
    verify(mapper).createParser(payload);
  }

  @Test
  void shouldPropagateExceptionWhenCreatingReaderFromByteArrayFails() throws IOException {
    // Arrange
    byte[] payload = "{\"test\": 1}".getBytes();
    boolean isTuple = false;
    IOException expectedException = new IOException("Parsing failed");

    when(mapper.createParser(payload)).thenThrow(expectedException);

    // Act & Assert
    Exception thrown = assertThrows(Exception.class, () -> parser.reader(payload, isTuple));
    assertEquals(expectedException, thrown);
  }

  @Test
  void shouldCreateReaderFromString() throws IOException {
    // Arrange
    String payload = "[{\"test\": 1}]";
    boolean isTuple = true;
    JsonParser jsonParser = mock(JsonParser.class);

    // FIX: Satisfy the JacksonTrpcReader validation by returning START_ARRAY
    when(jsonParser.nextToken()).thenReturn(JsonToken.START_ARRAY);
    when(mapper.createParser(payload)).thenReturn(jsonParser);

    // Act
    TrpcReader reader = parser.reader(payload, isTuple);

    // Assert
    assertNotNull(reader);
    assertTrue(reader instanceof JacksonTrpcReader);
    verify(mapper).createParser(payload);
    verify(jsonParser).nextToken();
  }

  @Test
  void shouldPropagateExceptionWhenCreatingReaderFromStringFails() throws IOException {
    // Arrange
    String payload = "[{\"test\": 1}]";
    boolean isTuple = true;
    IOException expectedException = new IOException("Parsing failed");

    when(mapper.createParser(payload)).thenThrow(expectedException);

    // Act & Assert
    Exception thrown = assertThrows(Exception.class, () -> parser.reader(payload, isTuple));
    assertEquals(expectedException, thrown);
  }
}
