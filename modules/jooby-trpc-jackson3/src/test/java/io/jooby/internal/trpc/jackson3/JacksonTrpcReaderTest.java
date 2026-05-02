/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.jackson3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.exception.MissingValueException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectReader;

class JacksonTrpcReaderTest {

  private JsonParser parser;

  @BeforeEach
  void setUp() {
    parser = mock(JsonParser.class);
  }

  // --- CONSTRUCTOR TESTS ---

  @Test
  void shouldInitializeSuccessfullyWhenIsTupleAndTokenIsStartArray() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);
    verify(parser, times(1)).nextToken();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenIsTupleAndTokenIsNotStartArray() {
    when(parser.nextToken()).thenReturn(JsonToken.START_OBJECT);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> new JacksonTrpcReader(parser, true));
    assertEquals("Expected tRPC tuple array", ex.getMessage());
  }

  // --- NULL CHECK (hasPeeked STATE LOGIC) ---

  @Test
  void shouldReturnTrueAndConsumePeekWhenNextIsNull() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NULL);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NULL);
    assertTrue(reader.nextIsNull("param1"));

    // Test that the state was consumed and it peeks again
    when(parser.nextToken()).thenReturn(JsonToken.VALUE_STRING);
    when(parser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
    assertFalse(reader.nextIsNull("param2"));

    verify(parser, times(3)).nextToken();
  }

  @Test
  void shouldReturnFalseAndRetainPeekWhenNextIsNotNull() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_STRING);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
    assertFalse(reader.nextIsNull("param1")); // Peeks and retains
    assertFalse(reader.nextIsNull("param1")); // Re-uses the peek

    verify(parser, times(2)).nextToken(); // Constructor + first peek
  }

  // --- ADVANCE LOGIC (TUPLE VS NON-TUPLE) ---

  @Test
  void shouldThrowMissingValueExceptionWhenNonTupleReadTwice() {
    when(parser.nextToken()).thenReturn(JsonToken.VALUE_STRING);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, false);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
    when(parser.getString()).thenReturn("first");

    assertEquals("first", reader.nextString("param1")); // First read works seamlessly

    assertThrows(
        MissingValueException.class, () -> reader.nextString("param2")); // Second read fails
  }

  @Test
  void shouldThrowMissingValueExceptionWhenTupleReachesEndArray() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.END_ARRAY);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    assertThrows(MissingValueException.class, () -> reader.nextString("param1"));
  }

  @Test
  void shouldThrowMissingValueExceptionWhenTupleReachesNullToken() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, null);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    assertThrows(MissingValueException.class, () -> reader.nextString("param1"));
  }

  // --- ENSURE NON-NULL LOGIC ---

  @Test
  void shouldThrowMissingValueExceptionIfAttemptingToReadExplicitNullValue() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NULL);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NULL);

    assertThrows(MissingValueException.class, () -> reader.nextInt("param"));
  }

  // --- VALUE EXTRACTORS ---

  @Test
  void shouldExtractIntSuccessfully() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NUMBER_INT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_INT);
    when(parser.getIntValue()).thenReturn(42);

    assertEquals(42, reader.nextInt("param"));
  }

  @Test
  void shouldExtractLongSuccessfully() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NUMBER_INT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_INT);
    when(parser.getLongValue()).thenReturn(999L);

    assertEquals(999L, reader.nextLong("param"));
  }

  @Test
  void shouldExtractBooleanSuccessfully() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_TRUE);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_TRUE);
    when(parser.getBooleanValue()).thenReturn(true);

    assertTrue(reader.nextBoolean("param"));
  }

  @Test
  void shouldExtractDoubleSuccessfully() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NUMBER_FLOAT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_FLOAT);
    when(parser.getDoubleValue()).thenReturn(3.14);

    assertEquals(3.14, reader.nextDouble("param"));
  }

  @Test
  void shouldExtractStringSuccessfully() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_STRING);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
    when(parser.getString()).thenReturn("hello");

    assertEquals("hello", reader.nextString("param"));
  }

  // --- OBJECT DECODING ---

  @Test
  void shouldExtractObjectSuccessfully() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.START_OBJECT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.START_OBJECT);

    ObjectReader objectReader = mock(ObjectReader.class);
    JacksonTrpcDecoder<Object> decoder = new JacksonTrpcDecoder<>(objectReader);
    Object expectedObject = new Object();

    when(objectReader.readValue(parser)).thenReturn(expectedObject);

    assertEquals(expectedObject, reader.nextObject("param", decoder));
  }

  // --- CLOSE ---

  @Test
  void shouldCloseParserSuccessfully() {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    reader.close();

    verify(parser, times(1)).close();
  }
}
