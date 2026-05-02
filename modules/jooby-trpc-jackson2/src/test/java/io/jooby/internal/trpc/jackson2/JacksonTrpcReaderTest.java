/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.jackson2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectReader;
import io.jooby.exception.MissingValueException;

class JacksonTrpcReaderTest {

  private JsonParser parser;

  @BeforeEach
  void setUp() {
    parser = mock(JsonParser.class);
  }

  // --- CONSTRUCTOR & NEXT TOKEN TESTS ---

  @Test
  void shouldInitializeSuccessfullyWhenIsTupleAndTokenIsStartArray() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);
    verify(parser, times(1)).nextToken();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenIsTupleAndTokenIsNotStartArray() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_OBJECT);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> new JacksonTrpcReader(parser, true));
    assertEquals("Expected tRPC tuple array", ex.getMessage());
  }

  @Test
  void shouldPropagateIOExceptionDuringInitialization() throws IOException {
    IOException expectedEx = new IOException("Init failed");
    when(parser.nextToken()).thenThrow(expectedEx);

    Exception ex = assertThrows(Exception.class, () -> new JacksonTrpcReader(parser, true));
    assertEquals(expectedEx, ex);
  }

  // --- NULL CHECK (hasPeeked STATE LOGIC) ---

  @Test
  void shouldReturnTrueAndConsumePeekWhenNextIsNull() throws IOException {
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
  void shouldReturnFalseAndRetainPeekWhenNextIsNotNull() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_STRING);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
    assertFalse(reader.nextIsNull("param1")); // Peeks and retains
    assertFalse(reader.nextIsNull("param1")); // Re-uses the peek

    verify(parser, times(2)).nextToken(); // Constructor + first peek
  }

  // --- ADVANCE LOGIC (TUPLE VS NON-TUPLE) ---

  @Test
  void shouldThrowMissingValueExceptionWhenNonTupleReadTwice() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.VALUE_STRING); // Init doesn't care if non-tuple
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, false);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
    when(parser.getText()).thenReturn("first");

    assertEquals("first", reader.nextString("param1")); // First read works seamlessly

    assertThrows(
        MissingValueException.class, () -> reader.nextString("param2")); // Second read fails
  }

  @Test
  void shouldThrowMissingValueExceptionWhenTupleReachesEndArray() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.END_ARRAY);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    assertThrows(MissingValueException.class, () -> reader.nextString("param1"));
  }

  @Test
  void shouldThrowMissingValueExceptionWhenTupleReachesNullToken() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, null);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    assertThrows(MissingValueException.class, () -> reader.nextString("param1"));
  }

  // --- ENSURE NON-NULL LOGIC ---

  @Test
  void shouldThrowMissingValueExceptionIfAttemptingToReadExplicitNullValue() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NULL);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NULL);

    assertThrows(MissingValueException.class, () -> reader.nextInt("param"));
  }

  // --- VALUE EXTRACTORS ---

  @Test
  void shouldExtractIntSuccessfully() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NUMBER_INT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_INT);
    when(parser.getIntValue()).thenReturn(42);

    assertEquals(42, reader.nextInt("param"));
  }

  @Test
  void shouldPropagateExceptionWhenExtractingIntFails() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NUMBER_INT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_INT);
    IOException expectedEx = new IOException("IO error");
    when(parser.getIntValue()).thenThrow(expectedEx);

    Exception ex = assertThrows(Exception.class, () -> reader.nextInt("param"));
    assertEquals(expectedEx, ex);
  }

  @Test
  void shouldExtractLongSuccessfully() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NUMBER_INT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_INT);
    when(parser.getLongValue()).thenReturn(999L);

    assertEquals(999L, reader.nextLong("param"));
  }

  @Test
  void shouldPropagateExceptionWhenExtractingLongFails() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NUMBER_INT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_INT);
    IOException expectedEx = new IOException("IO error");
    when(parser.getLongValue()).thenThrow(expectedEx);

    Exception ex = assertThrows(Exception.class, () -> reader.nextLong("param"));
    assertEquals(expectedEx, ex);
  }

  @Test
  void shouldExtractBooleanSuccessfully() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_TRUE);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_TRUE);
    when(parser.getBooleanValue()).thenReturn(true);

    assertTrue(reader.nextBoolean("param"));
  }

  @Test
  void shouldPropagateExceptionWhenExtractingBooleanFails() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_TRUE);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_TRUE);
    IOException expectedEx = new IOException("IO error");
    when(parser.getBooleanValue()).thenThrow(expectedEx);

    Exception ex = assertThrows(Exception.class, () -> reader.nextBoolean("param"));
    assertEquals(expectedEx, ex);
  }

  @Test
  void shouldExtractDoubleSuccessfully() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NUMBER_FLOAT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_FLOAT);
    when(parser.getDoubleValue()).thenReturn(3.14);

    assertEquals(3.14, reader.nextDouble("param"));
  }

  @Test
  void shouldPropagateExceptionWhenExtractingDoubleFails() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_NUMBER_FLOAT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_FLOAT);
    IOException expectedEx = new IOException("IO error");
    when(parser.getDoubleValue()).thenThrow(expectedEx);

    Exception ex = assertThrows(Exception.class, () -> reader.nextDouble("param"));
    assertEquals(expectedEx, ex);
  }

  @Test
  void shouldPropagateExceptionWhenExtractingStringFails() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.VALUE_STRING);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
    IOException expectedEx = new IOException("IO error");
    when(parser.getText()).thenThrow(expectedEx);

    Exception ex = assertThrows(Exception.class, () -> reader.nextString("param"));
    assertEquals(expectedEx, ex);
  }

  // --- OBJECT DECODING ---

  @Test
  void shouldExtractObjectSuccessfully() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.START_OBJECT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.START_OBJECT);

    ObjectReader objectReader = mock(ObjectReader.class);
    JacksonTrpcDecoder<Object> decoder = new JacksonTrpcDecoder<>(objectReader);
    Object expectedObject = new Object();

    when(objectReader.readValue(parser)).thenReturn(expectedObject);

    assertEquals(expectedObject, reader.nextObject("param", decoder));
  }

  @Test
  void shouldPropagateExceptionWhenExtractingObjectFails() throws IOException {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY, JsonToken.START_OBJECT);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    when(parser.currentToken()).thenReturn(JsonToken.START_OBJECT);

    ObjectReader objectReader = mock(ObjectReader.class);
    JacksonTrpcDecoder<Object> decoder = new JacksonTrpcDecoder<>(objectReader);
    IOException expectedEx = new IOException("IO error");

    when(objectReader.readValue(parser)).thenThrow(expectedEx);

    Exception ex = assertThrows(Exception.class, () -> reader.nextObject("param", decoder));
    assertEquals(expectedEx, ex);
  }

  // --- CLOSE ---

  @Test
  void shouldCloseParserSuccessfully() throws Exception {
    when(parser.nextToken()).thenReturn(JsonToken.START_ARRAY);
    JacksonTrpcReader reader = new JacksonTrpcReader(parser, true);

    reader.close();

    verify(parser, times(1)).close();
  }
}
