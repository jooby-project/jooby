/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

class JacksonJsonCodecTest {

  private ObjectMapper mapper;
  private JacksonJsonCodec codec;

  @BeforeEach
  void setUp() {
    mapper = mock(ObjectMapper.class);
    codec = new JacksonJsonCodec(mapper);
  }

  // --- DECODE BY CLASS TESTS ---

  @Test
  void shouldDecodeClassSuccessfully() throws JsonProcessingException {
    // Arrange
    String json = "{\"key\":\"value\"}";
    Object expectedResult = new Object();
    when(mapper.readValue(json, Object.class)).thenReturn(expectedResult);

    // Act
    Object actualResult = codec.decode(json, Object.class);

    // Assert
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void shouldPropagateExceptionWhenDecodingClassFails() throws JsonProcessingException {
    // Arrange
    String json = "invalid json";
    JsonProcessingException expectedException = mock(JsonProcessingException.class);
    when(mapper.readValue(json, Object.class)).thenThrow(expectedException);

    // Act & Assert
    // SneakyThrows propagates the exact checked exception without wrapping it
    Exception thrown = assertThrows(Exception.class, () -> codec.decode(json, Object.class));
    assertEquals(expectedException, thrown);
  }

  // --- DECODE BY TYPE TESTS ---

  @Test
  void shouldDecodeTypeSuccessfully() throws JsonProcessingException {
    // Arrange
    String json = "[\"value\"]";
    Type type = List.class;
    Object expectedResult = new Object();

    TypeFactory typeFactory = mock(TypeFactory.class);
    JavaType javaType = mock(JavaType.class);

    when(mapper.getTypeFactory()).thenReturn(typeFactory);
    when(typeFactory.constructType(type)).thenReturn(javaType);
    when(mapper.readValue(json, javaType)).thenReturn(expectedResult);

    // Act
    Object actualResult = codec.decode(json, type);

    // Assert
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void shouldPropagateExceptionWhenDecodingTypeFails() throws JsonProcessingException {
    // Arrange
    String json = "invalid json";
    Type type = List.class;

    TypeFactory typeFactory = mock(TypeFactory.class);
    JavaType javaType = mock(JavaType.class);
    JsonProcessingException expectedException = mock(JsonProcessingException.class);

    when(mapper.getTypeFactory()).thenReturn(typeFactory);
    when(typeFactory.constructType(type)).thenReturn(javaType);
    when(mapper.readValue(json, javaType)).thenThrow(expectedException);

    // Act & Assert
    Exception thrown = assertThrows(Exception.class, () -> codec.decode(json, type));
    assertEquals(expectedException, thrown);
  }

  // --- ENCODE TESTS ---

  @Test
  void shouldEncodeSuccessfully() throws JsonProcessingException {
    // Arrange
    Object value = new Object();
    String expectedJson = "{}";
    when(mapper.writeValueAsString(value)).thenReturn(expectedJson);

    // Act
    String actualJson = codec.encode(value);

    // Assert
    assertEquals(expectedJson, actualJson);
  }

  @Test
  void shouldPropagateExceptionWhenEncodingFails() throws JsonProcessingException {
    // Arrange
    Object value = new Object();
    JsonProcessingException expectedException = mock(JsonProcessingException.class);
    when(mapper.writeValueAsString(value)).thenThrow(expectedException);

    // Act & Assert
    Exception thrown = assertThrows(Exception.class, () -> codec.encode(value));
    assertEquals(expectedException, thrown);
  }
}
