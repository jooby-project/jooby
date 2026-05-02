/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.jackson3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NullNode;

class JacksonJsonRpcDecoderTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setup() {
    mapper = new ObjectMapper();
  }

  // --- SUCCESS TESTS ---

  @Test
  void testDecode_Success_SimpleType() {
    JacksonJsonRpcDecoder<String> decoder = new JacksonJsonRpcDecoder<>(mapper, String.class);
    JsonNode node = mapper.valueToTree("hello world");

    String result = decoder.decode("testParam", node);

    assertEquals("hello world", result);
  }

  @Test
  void testDecode_Success_ComplexType() {
    JacksonJsonRpcDecoder<DummyUser> decoder = new JacksonJsonRpcDecoder<>(mapper, DummyUser.class);

    // Create a JSON object representing a User
    JsonNode node = mapper.createObjectNode().put("id", 1).put("name", "edgar");

    DummyUser result = decoder.decode("userParam", node);

    assertEquals(1, result.id);
    assertEquals("edgar", result.name);
  }

  // --- MISSING VALUE (WRAPPED IN TYPE MISMATCH) TESTS ---

  @Test
  void testDecode_ThrowsTypeMismatchException_WrappingMissingValue_WhenNodeIsJavaNull() {
    JacksonJsonRpcDecoder<String> decoder = new JacksonJsonRpcDecoder<>(mapper, String.class);

    TypeMismatchException ex =
        assertThrows(TypeMismatchException.class, () -> decoder.decode("nullParam", null));

    // Verify it wrapped the MissingValueException
    assertEquals(MissingValueException.class, ex.getCause().getClass());
    assertEquals("nullParam", ex.getName());
  }

  @Test
  void testDecode_ThrowsTypeMismatchException_WrappingMissingValue_WhenNodeIsJacksonNullNode() {
    JacksonJsonRpcDecoder<String> decoder = new JacksonJsonRpcDecoder<>(mapper, String.class);
    JsonNode nullNode = NullNode.getInstance();

    TypeMismatchException ex =
        assertThrows(TypeMismatchException.class, () -> decoder.decode("nullNodeParam", nullNode));

    assertEquals(MissingValueException.class, ex.getCause().getClass());
    assertEquals("nullNodeParam", ex.getName());
  }

  @Test
  void testDecode_ThrowsTypeMismatchException_WrappingMissingValue_WhenNodeIsJacksonMissingNode() {
    JacksonJsonRpcDecoder<String> decoder = new JacksonJsonRpcDecoder<>(mapper, String.class);
    JsonNode missingNode = MissingNode.getInstance();

    TypeMismatchException ex =
        assertThrows(
            TypeMismatchException.class, () -> decoder.decode("missingNodeParam", missingNode));

    assertEquals(MissingValueException.class, ex.getCause().getClass());
    assertEquals("missingNodeParam", ex.getName());
  }

  // --- TYPE MISMATCH EXCEPTION TESTS ---

  @Test
  void testDecode_ThrowsTypeMismatchException_WhenTreeToValueFails() {
    // Attempt to map an ObjectNode (JSON Object) to an Integer
    JacksonJsonRpcDecoder<Integer> decoder = new JacksonJsonRpcDecoder<>(mapper, Integer.class);
    JsonNode invalidNode = mapper.createObjectNode().put("key", "value");

    TypeMismatchException ex =
        assertThrows(TypeMismatchException.class, () -> decoder.decode("intParam", invalidNode));

    // Verify the parameter name was correctly propagated to the exception
    assertEquals("intParam", ex.getName());
  }

  @Test
  void testDecode_ThrowsTypeMismatchException_WhenNodeIsNotAJsonNode() {
    JacksonJsonRpcDecoder<String> decoder = new JacksonJsonRpcDecoder<>(mapper, String.class);

    TypeMismatchException ex =
        assertThrows(
            TypeMismatchException.class,
            () -> decoder.decode("badCastParam", "This is a raw string, not a JsonNode"));

    assertEquals("badCastParam", ex.getName());
    assertEquals(ClassCastException.class, ex.getCause().getClass());
  }

  // --- HELPER CLASS ---

  static class DummyUser {
    public int id;
    public String name;
  }
}
