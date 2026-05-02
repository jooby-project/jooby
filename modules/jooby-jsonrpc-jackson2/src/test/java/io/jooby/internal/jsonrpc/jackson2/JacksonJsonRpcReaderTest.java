/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.jackson2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.jsonrpc.JsonRpcDecoder;

class JacksonJsonRpcReaderTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setup() {
    mapper = new ObjectMapper();
  }

  // --- NULL & VALUE NODE TESTS (Edge Cases) ---

  @Test
  void testNullParams() {
    JacksonJsonRpcReader reader = new JacksonJsonRpcReader(null);

    assertTrue(reader.nextIsNull("anyKey"));
    assertThrows(MissingValueException.class, () -> reader.nextInt("anyKey"));
  }

  @Test
  void testValueNodeParams_NotObjectOrArray() {
    // Tests the fallback when params is neither an array nor an object
    JsonNode valueNode = mapper.valueToTree("just a string");
    JacksonJsonRpcReader reader = new JacksonJsonRpcReader(valueNode);

    assertTrue(reader.nextIsNull("anyKey"));
    assertThrows(MissingValueException.class, () -> reader.nextInt("anyKey"));
  }

  @Test
  void testRequireNode_MissingNodeBranch() {
    // Uses Mockito to explicitly force the isMissingNode() = true branch.
    // This is defensive logic in JacksonJsonRpcReader that is hard to trigger organically
    // because ObjectNode.get() typically returns null for missing keys.
    JsonNode root = mock(JsonNode.class);
    JsonNode child = mock(JsonNode.class);

    when(root.isObject()).thenReturn(true);
    when(root.get("missingKey")).thenReturn(child);
    when(child.isNull()).thenReturn(false);
    when(child.isMissingNode()).thenReturn(true);

    JacksonJsonRpcReader reader = new JacksonJsonRpcReader(root);

    assertThrows(MissingValueException.class, () -> reader.nextString("missingKey"));
  }

  // --- ARRAY MODE TESTS ---

  @Test
  void testArrayMode_ValidTypes() {
    ArrayNode array = mapper.createArrayNode();
    array.add(42);
    array.add(1234567890123L);
    array.add(true);
    array.add(3.14d);
    array.add("hello array");

    ObjectNode nestedObj = mapper.createObjectNode();
    nestedObj.put("key", "val");
    array.add(nestedObj);

    JacksonJsonRpcReader reader = new JacksonJsonRpcReader(array);

    // Asserting valid reads. Name parameter is ignored in array mode.
    assertEquals(42, reader.nextInt("ignored"));
    assertEquals(1234567890123L, reader.nextLong("ignored"));
    assertTrue(reader.nextBoolean("ignored"));
    assertEquals(3.14d, reader.nextDouble("ignored"));
    assertEquals("hello array", reader.nextString("ignored"));

    JsonRpcDecoder<String> decoder = (name, node) -> ((JsonNode) node).get("key").asText();
    assertEquals("val", reader.nextObject("ignored", decoder));

    // Call close for 100% coverage (it's a no-op)
    reader.close();
  }

  @Test
  void testArrayMode_NextIsNull() {
    ArrayNode array = mapper.createArrayNode();
    array.addNull();

    JacksonJsonRpcReader reader = new JacksonJsonRpcReader(array);

    // peekNode doesn't advance the index
    assertTrue(reader.nextIsNull("ignored"));
    assertTrue(reader.nextIsNull("ignored"));
  }

  // --- OBJECT MODE TESTS ---

  @Test
  void testObjectMode_ValidTypes() {
    ObjectNode obj = mapper.createObjectNode();
    obj.put("i", 42);
    obj.put("l", 1234567890123L);
    obj.put("b", true);
    obj.put("d", 3.14d);
    obj.put("s", "hello object");

    ObjectNode nestedObj = mapper.createObjectNode();
    nestedObj.put("key", "val");
    obj.set("o", nestedObj);

    JacksonJsonRpcReader reader = new JacksonJsonRpcReader(obj);

    assertFalse(reader.nextIsNull("i"));

    assertEquals(42, reader.nextInt("i"));
    assertEquals(1234567890123L, reader.nextLong("l"));
    assertTrue(reader.nextBoolean("b"));
    assertEquals(3.14d, reader.nextDouble("d"));
    assertEquals("hello object", reader.nextString("s"));

    JsonRpcDecoder<String> decoder = (name, node) -> ((JsonNode) node).get("key").asText();
    assertEquals("val", reader.nextObject("o", decoder));
  }

  @Test
  void testObjectMode_MissingAndNullValues() {
    ObjectNode obj = mapper.createObjectNode();
    obj.putNull("nullField");
    // "missingField" does not exist

    JacksonJsonRpcReader reader = new JacksonJsonRpcReader(obj);

    assertTrue(reader.nextIsNull("nullField"));
    assertTrue(reader.nextIsNull("missingField"));

    assertThrows(MissingValueException.class, () -> reader.nextInt("nullField"));
    assertThrows(MissingValueException.class, () -> reader.nextInt("missingField"));
  }

  // --- TYPE MISMATCH EXCEPTION TESTS ---

  @Test
  void testTypeMismatches_ThrowsException() {
    ObjectNode obj = mapper.createObjectNode();
    obj.put("stringField", "not a number");
    obj.put("intField", 42);

    JacksonJsonRpcReader reader = new JacksonJsonRpcReader(obj);

    assertThrows(TypeMismatchException.class, () -> reader.nextInt("stringField"));
    assertThrows(TypeMismatchException.class, () -> reader.nextLong("stringField"));
    assertThrows(TypeMismatchException.class, () -> reader.nextBoolean("stringField"));
    assertThrows(TypeMismatchException.class, () -> reader.nextDouble("stringField"));

    // Int cannot be read as a string
    assertThrows(TypeMismatchException.class, () -> reader.nextString("intField"));
  }
}
