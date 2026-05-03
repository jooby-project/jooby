/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.avaje.jsonb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.avaje.json.JsonReader;
import io.jooby.exception.MissingValueException;

class AvajeTrpcReaderTest {

  private JsonReader reader;

  @BeforeEach
  void setUp() {
    reader = mock(JsonReader.class);
  }

  // --- CONSTRUCTOR TESTS ---

  @Test
  void shouldBeginArrayWhenIsTuple() {
    new AvajeTrpcReader(reader, true);
    verify(reader).beginArray();
  }

  @Test
  void shouldNotBeginArrayWhenNotTuple() {
    new AvajeTrpcReader(reader, false);
    verify(reader, never()).beginArray();
  }

  // --- NULL CHECK (hasPeeked STATE LOGIC) ---

  @Test
  void shouldReturnTrueAndConsumePeekWhenNextIsNull() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, true);
    when(reader.hasNextElement()).thenReturn(true);
    when(reader.isNullValue()).thenReturn(true);

    assertTrue(trpcReader.nextIsNull("param1"));
    verify(reader).skipValue();

    // Verify it consumes the peek and checks again on the next call
    assertTrue(trpcReader.nextIsNull("param2"));
    verify(reader, times(2)).hasNextElement();
  }

  @Test
  void shouldReturnFalseAndRetainPeekWhenNextIsNotNull() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, true);
    when(reader.hasNextElement()).thenReturn(true);
    when(reader.isNullValue()).thenReturn(false);

    assertFalse(trpcReader.nextIsNull("param1")); // Sets hasPeeked to true
    verify(reader, never()).skipValue();

    // When calling a value extractor next, it should use the peeked state
    when(reader.readInt()).thenReturn(42);
    assertEquals(42, trpcReader.nextInt("param1"));

    // hasNextElement was only called once during the initial peek
    verify(reader, times(1)).hasNextElement();
  }

  // --- ADVANCE LOGIC (TUPLE VS NON-TUPLE) ---

  @Test
  void shouldThrowMissingValueWhenTupleHasNoMoreElements() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, true);
    when(reader.hasNextElement()).thenReturn(false);

    assertThrows(MissingValueException.class, () -> trpcReader.nextInt("param1"));
  }

  @Test
  void shouldThrowMissingValueWhenNonTupleReadTwice() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, false);
    when(reader.readInt()).thenReturn(1);

    // First read succeeds (isFirstRead = true)
    assertEquals(1, trpcReader.nextInt("param1"));

    // Second read fails (isFirstRead = false)
    assertThrows(MissingValueException.class, () -> trpcReader.nextInt("param2"));
  }

  // --- ENSURE NON-NULL LOGIC ---

  @Test
  void shouldThrowMissingValueWhenExplicitNullEncountered() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, false);
    when(reader.isNullValue()).thenReturn(true);

    assertThrows(MissingValueException.class, () -> trpcReader.nextInt("param"));
  }

  // --- VALUE EXTRACTORS ---

  @Test
  void shouldExtractInt() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, false);
    when(reader.readInt()).thenReturn(42);
    assertEquals(42, trpcReader.nextInt("param"));
  }

  @Test
  void shouldExtractLong() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, false);
    when(reader.readLong()).thenReturn(42L);
    assertEquals(42L, trpcReader.nextLong("param"));
  }

  @Test
  void shouldExtractBoolean() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, false);
    when(reader.readBoolean()).thenReturn(true);
    assertTrue(trpcReader.nextBoolean("param"));
  }

  @Test
  void shouldExtractDouble() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, false);
    when(reader.readDouble()).thenReturn(42.5);
    assertEquals(42.5, trpcReader.nextDouble("param"));
  }

  @Test
  void shouldExtractString() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, false);
    when(reader.readString()).thenReturn("test");
    assertEquals("test", trpcReader.nextString("param"));
  }

  // --- OBJECT DECODING ---

  @Test
  @SuppressWarnings("unchecked")
  void shouldExtractObjectViaDecoder() throws Exception {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, false);

    AvajeTrpcDecoder<Object> decoder = mock(AvajeTrpcDecoder.class);

    io.avaje.jsonb.JsonType<Object> mockAdapter = mock(io.avaje.jsonb.JsonType.class);

    // Use reflection to inject the mocked JsonType into the decoder field
    java.lang.reflect.Field field = AvajeTrpcDecoder.class.getDeclaredField("typeAdapter");
    field.setAccessible(true);
    field.set(decoder, mockAdapter);

    Object expectedObject = new Object();
    when(mockAdapter.fromJson(reader)).thenReturn(expectedObject);

    assertEquals(expectedObject, trpcReader.nextObject("param", decoder));
  }

  // --- CLOSE ---

  @Test
  void shouldCloseAndEndArrayWhenTuple() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, true);
    trpcReader.close();

    verify(reader).endArray();
    verify(reader).close();
  }

  @Test
  void shouldCloseWithoutEndingArrayWhenNotTuple() {
    AvajeTrpcReader trpcReader = new AvajeTrpcReader(reader, false);
    trpcReader.close();

    verify(reader, never()).endArray();
    verify(reader).close();
  }
}
