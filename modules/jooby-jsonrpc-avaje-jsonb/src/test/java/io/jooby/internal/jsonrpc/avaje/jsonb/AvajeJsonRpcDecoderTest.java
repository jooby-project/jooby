/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.avaje.jsonb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class AvajeJsonRpcDecoderTest {

  @Mock Jsonb jsonb;
  @Mock JsonType stringJsonType;
  @Mock JsonType intJsonType;

  // --- SUCCESS TESTS ---

  @Test
  void testDecode_Success() {
    // FIX: Cast to java.lang.reflect.Type to match the exact overload called in production
    when(jsonb.type((Type) String.class)).thenReturn(stringJsonType);

    // The decoder converts the raw object to a JSON string, then asks the adapter to parse it
    when(jsonb.toJson("hello")).thenReturn("\"hello\"");
    when(stringJsonType.fromJson("\"hello\"")).thenReturn("hello");

    AvajeJsonRpcDecoder<String> decoder = new AvajeJsonRpcDecoder<>(jsonb, String.class);
    String result = decoder.decode("testParam", "hello");

    assertEquals("hello", result);
  }

  // --- MISSING VALUE (WRAPPED IN TYPE MISMATCH) TESTS ---

  @Test
  void testDecode_ThrowsTypeMismatchException_WrappingMissingValue_WhenNodeIsNull() {
    // FIX: Cast to java.lang.reflect.Type
    when(jsonb.type((Type) String.class)).thenReturn(stringJsonType);

    AvajeJsonRpcDecoder<String> decoder = new AvajeJsonRpcDecoder<>(jsonb, String.class);

    // Because the MissingValueException is thrown inside a try block that catches (Exception x),
    // it gets immediately trapped and wrapped in a TypeMismatchException.
    TypeMismatchException ex =
        assertThrows(TypeMismatchException.class, () -> decoder.decode("nullParam", null));

    // Verify it securely wrapped the MissingValueException
    assertEquals(MissingValueException.class, ex.getCause().getClass());
    assertEquals("nullParam", ex.getName());
  }

  // --- TYPE MISMATCH EXCEPTION TESTS ---

  @Test
  void testDecode_ThrowsTypeMismatchException_WhenAvajeConversionFails() {
    // FIX: Cast to java.lang.reflect.Type
    when(jsonb.type((Type) Integer.class)).thenReturn(intJsonType);
    when(jsonb.toJson("not an integer")).thenReturn("\"not an integer\"");

    // Simulate Avaje rejecting the badly typed JSON payload
    when(intJsonType.fromJson("\"not an integer\""))
        .thenThrow(new IllegalArgumentException("Invalid number format"));

    AvajeJsonRpcDecoder<Integer> decoder = new AvajeJsonRpcDecoder<>(jsonb, Integer.class);

    TypeMismatchException ex =
        assertThrows(
            TypeMismatchException.class, () -> decoder.decode("intParam", "not an integer"));

    // Verify the parameter name was accurately propagated to the resulting exception
    assertEquals("intParam", ex.getName());
    assertEquals(IllegalArgumentException.class, ex.getCause().getClass());
  }
}
