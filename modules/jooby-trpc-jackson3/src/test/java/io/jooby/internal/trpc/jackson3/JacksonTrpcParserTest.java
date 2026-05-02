/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.jackson3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Type;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.trpc.TrpcDecoder;
import io.jooby.trpc.TrpcReader;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

class JacksonTrpcParserTest {

  private ObjectMapper mapper;
  private JacksonTrpcParser parser;

  @BeforeEach
  void setUp() {
    mapper = mock(ObjectMapper.class);
    parser = new JacksonTrpcParser();
    parser.setMapper(mapper);
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
    RuntimeException expectedException = new RuntimeException("Parsing failed");

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
    RuntimeException expectedException = new RuntimeException("Parsing failed");

    when(mapper.createParser(payload)).thenThrow(expectedException);

    // Act & Assert
    Exception thrown = assertThrows(Exception.class, () -> parser.reader(payload, isTuple));
    assertEquals(expectedException, thrown);
  }
}
