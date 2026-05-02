/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.avaje.jsonb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.jsonrpc.JsonRpcDecoder;

class AvajeJsonRpcReaderTest {

  // --- CONSTRUCTOR / INVALID PARAM TYPE TESTS ---

  @Test
  void testConstructor_NullParams() {
    AvajeJsonRpcReader reader = new AvajeJsonRpcReader(null);

    // Will fall through all conditions and return null
    assertTrue(reader.nextIsNull("any"));
    assertThrows(MissingValueException.class, () -> reader.nextInt("any"));
  }

  @Test
  void testConstructor_UnrecognizedType() {
    AvajeJsonRpcReader reader = new AvajeJsonRpcReader("I am just a string, not a Map or List");

    assertTrue(reader.nextIsNull("any"));
    assertThrows(MissingValueException.class, () -> reader.nextLong("any"));
  }

  // --- LIST MODE (ARRAY PARAMS) TESTS ---

  @Test
  void testListMode_ValidTypesAndSequentialReading() {
    Map<String, Object> innerObj = Map.of("key", "val");
    List<Object> list = Arrays.asList(42, 1234567890123L, true, 3.14d, "hello list", innerObj);
    AvajeJsonRpcReader reader = new AvajeJsonRpcReader(list);

    // peek should not advance the internal index
    assertFalse(reader.nextIsNull("ignored-name"));
    assertFalse(reader.nextIsNull("ignored-name"));

    assertEquals(42, reader.nextInt("ignored"));
    assertEquals(1234567890123L, reader.nextLong("ignored"));
    assertTrue(reader.nextBoolean("ignored"));
    assertEquals(3.14d, reader.nextDouble("ignored"));
    assertEquals("hello list", reader.nextString("ignored"));

    @SuppressWarnings("unchecked")
    JsonRpcDecoder<Map<String, Object>> decoder = mock(JsonRpcDecoder.class);
    when(decoder.decode(eq("ignored"), any())).thenReturn(innerObj);

    assertEquals(innerObj, reader.nextObject("ignored", decoder));
  }

  @Test
  void testListMode_NullElement_ThrowsMissingValue() {
    List<Object> list = new ArrayList<>();
    list.add(null);
    AvajeJsonRpcReader reader = new AvajeJsonRpcReader(list);

    assertTrue(reader.nextIsNull("ignored"));
    assertThrows(MissingValueException.class, () -> reader.nextInt("ignored"));
  }

  @Test
  void testListMode_OutOfBounds_ThrowsMissingValue() {
    List<Object> list = List.of(100);
    AvajeJsonRpcReader reader = new AvajeJsonRpcReader(list);

    assertEquals(100, reader.nextInt("ignored"));

    // Index is now out of bounds
    assertTrue(reader.nextIsNull("ignored"));
    assertThrows(MissingValueException.class, () -> reader.nextInt("ignored"));
  }

  // --- MAP MODE (NAMED PARAMS) TESTS ---

  @Test
  void testMapMode_ValidTypes() {
    Map<String, Object> map = new HashMap<>();
    map.put("i", 42);
    map.put("l", 1234567890123L);
    map.put("b", true);
    map.put("d", 3.14d);
    map.put("s", "hello map");

    Map<String, Object> innerObj = Map.of("key", "val");
    map.put("o", innerObj);

    AvajeJsonRpcReader reader = new AvajeJsonRpcReader(map);

    assertFalse(reader.nextIsNull("i"));
    assertEquals(42, reader.nextInt("i"));
    assertEquals(1234567890123L, reader.nextLong("l"));
    assertTrue(reader.nextBoolean("b"));
    assertEquals(3.14d, reader.nextDouble("d"));
    assertEquals("hello map", reader.nextString("s"));

    @SuppressWarnings("unchecked")
    JsonRpcDecoder<Map<String, Object>> decoder = mock(JsonRpcDecoder.class);
    when(decoder.decode(eq("o"), eq(innerObj))).thenReturn(innerObj);

    assertEquals(innerObj, reader.nextObject("o", decoder));
  }

  @Test
  void testMapMode_MissingOrNullKey_ThrowsMissingValue() {
    Map<String, Object> map = new HashMap<>();
    map.put("nullKey", null);
    AvajeJsonRpcReader reader = new AvajeJsonRpcReader(map);

    assertTrue(reader.nextIsNull("nullKey"));
    assertTrue(reader.nextIsNull("missingKey"));

    MissingValueException ex1 =
        assertThrows(MissingValueException.class, () -> reader.nextInt("nullKey"));
    assertEquals("Missing value: 'nullKey'", ex1.getMessage());

    MissingValueException ex2 =
        assertThrows(MissingValueException.class, () -> reader.nextLong("missingKey"));
    assertEquals("Missing value: 'missingKey'", ex2.getMessage());
  }

  // --- TYPE MISMATCH EXCEPTION TESTS ---

  @Test
  void testTypeMismatches_ThrowsException() {
    Map<String, Object> map = new HashMap<>();
    map.put("strVal", "I am not a number");
    map.put("intVal", 42);

    AvajeJsonRpcReader reader = new AvajeJsonRpcReader(map);

    // Reading a String as a primitive
    assertThrows(TypeMismatchException.class, () -> reader.nextInt("strVal"));
    assertThrows(TypeMismatchException.class, () -> reader.nextLong("strVal"));
    assertThrows(TypeMismatchException.class, () -> reader.nextDouble("strVal"));
    assertThrows(TypeMismatchException.class, () -> reader.nextBoolean("strVal"));

    // nextString implicitly calls .toString(), so reading an int as a string technically
    // succeeds in this implementation ("42"), meaning no TypeMismatchException happens for string
    // fallback.
    assertEquals("42", reader.nextString("intVal"));
  }

  // --- MISC TESTS ---

  @Test
  void testClose() {
    AvajeJsonRpcReader reader = new AvajeJsonRpcReader(null);
    // Method is a no-op, call just to secure 100% line coverage
    reader.close();
  }
}
