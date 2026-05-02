/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.jackson2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.jsonrpc.JsonRpcRequest;

class JacksonJsonRpcRequestDeserializerTest {

  private ObjectMapper mapper;
  private JacksonJsonRpcRequestDeserializer deserializer;

  @BeforeEach
  void setup() {
    mapper = new ObjectMapper();
    deserializer = new JacksonJsonRpcRequestDeserializer();
  }

  private JsonRpcRequest deserialize(String json) throws IOException {
    JsonParser parser = new JsonFactory().createParser(json);
    parser.setCodec(mapper);
    return deserializer.deserialize(parser, mapper.getDeserializationContext());
  }

  // --- BATCH PARSING TESTS ---

  @Test
  void testDeserialize_EmptyArray_ReturnsInvalidRequestFlag() throws IOException {
    String json = "[]";
    JsonRpcRequest req = deserialize(json);

    // Spec dictates an empty array is an Invalid Request, rendered as a SINGLE error object
    assertNull(req.getMethod());
    assertFalse(req.isBatch());
  }

  @Test
  void testDeserialize_PopulatedArray_ReturnsBatchRequest() throws IOException {
    String json =
        "[\n"
            + "  {\"jsonrpc\": \"2.0\", \"method\": \"sum\", \"params\": [1,2,4], \"id\": \"1\"},\n"
            + "  {\"jsonrpc\": \"2.0\", \"method\": \"notify_hello\", \"params\": [7]}\n"
            + "]";

    JsonRpcRequest req = deserialize(json);

    assertTrue(req.isBatch());
    assertEquals(2, req.getRequests().size());

    JsonRpcRequest first = req.getRequests().get(0);
    assertEquals("2.0", first.getJsonrpc());
    assertEquals("sum", first.getMethod());
    assertEquals("1", first.getId());
    assertNotNull(first.getParams());

    JsonRpcRequest second = req.getRequests().get(1);
    assertEquals("2.0", second.getJsonrpc());
    assertEquals("notify_hello", second.getMethod());
    assertNull(second.getId()); // Notification
    assertNotNull(second.getParams());
  }

  // --- SINGLE REQUEST PARSING TESTS (Object validation) ---

  @Test
  void testParseSingle_ValidRequest_WithIdAndParamsArray() throws IOException {
    String json =
        "{\"jsonrpc\": \"2.0\", \"method\": \"subtract\", \"params\": [42, 23], \"id\": 1}";
    JsonRpcRequest req = deserialize(json);

    assertFalse(req.isBatch());
    assertEquals("2.0", req.getJsonrpc());
    assertEquals("subtract", req.getMethod());
    assertEquals(1, req.getId());
    assertTrue(((com.fasterxml.jackson.databind.JsonNode) req.getParams()).isArray());
  }

  @Test
  void testParseSingle_ValidRequest_WithIdAndParamsObject() throws IOException {
    String json =
        "{\"jsonrpc\": \"2.0\", \"method\": \"subtract\", \"params\": {\"subtrahend\": 23,"
            + " \"minuend\": 42}, \"id\": \"req-2\"}";
    JsonRpcRequest req = deserialize(json);

    assertFalse(req.isBatch());
    assertEquals("2.0", req.getJsonrpc());
    assertEquals("subtract", req.getMethod());
    assertEquals("req-2", req.getId());
    assertTrue(((com.fasterxml.jackson.databind.JsonNode) req.getParams()).isObject());
  }

  @Test
  void testParseSingle_ValidNotification_NoId() throws IOException {
    String json = "{\"jsonrpc\": \"2.0\", \"method\": \"update\", \"params\": [1,2,3]}";
    JsonRpcRequest req = deserialize(json);

    assertEquals("2.0", req.getJsonrpc());
    assertEquals("update", req.getMethod());
    assertNull(req.getId());
    assertNotNull(req.getParams());
  }

  // --- ERROR SCENARIOS (Triggers Invalid Request via null method) ---

  @Test
  void testParseSingle_NotAnObject_ReturnsInvalidRequest() throws IOException {
    // Top-level payload is just a primitive
    String json = "42";
    JsonRpcRequest req = deserialize(json);

    assertNull(req.getMethod());
  }

  @Test
  void testParseSingle_MissingVersion_ReturnsInvalidRequest() throws IOException {
    String json = "{\"method\": \"update\", \"params\": [1,2,3]}";
    JsonRpcRequest req = deserialize(json);

    assertNull(req.getMethod());
  }

  @Test
  void testParseSingle_WrongVersion_ReturnsInvalidRequest() throws IOException {
    String json = "{\"jsonrpc\": \"1.0\", \"method\": \"update\", \"params\": [1,2,3]}";
    JsonRpcRequest req = deserialize(json);

    assertNull(req.getMethod());
  }

  @Test
  void testParseSingle_MissingMethod_ReturnsInvalidRequest() throws IOException {
    String json = "{\"jsonrpc\": \"2.0\", \"params\": [1,2,3], \"id\": 1}";
    JsonRpcRequest req = deserialize(json);

    // ID is retained for error echoing, but method is null
    assertEquals(1, req.getId());
    assertNull(req.getMethod());
  }

  @Test
  void testParseSingle_InvalidParamsType_ReturnsInvalidRequest() throws IOException {
    // Params must be an Array or an Object. A primitive string is invalid.
    String json =
        "{\"jsonrpc\": \"2.0\", \"method\": \"update\", \"params\": \"not an array or object\","
            + " \"id\": 1}";
    JsonRpcRequest req = deserialize(json);

    // ID is retained for error echoing, but method is null
    assertEquals(1, req.getId());
    assertNull(req.getMethod());
  }

  // --- ID EXTRACTION COVERAGE ---

  @Test
  void testParseSingle_IdIsNull_ReturnsNullId() throws IOException {
    String json = "{\"jsonrpc\": \"2.0\", \"method\": \"update\", \"params\": [], \"id\": null}";
    JsonRpcRequest req = deserialize(json);

    assertNull(req.getId());
  }

  @Test
  void testParseSingle_IdIsBoolean_Ignored() throws IOException {
    // Only numbers and strings are valid IDs. Booleans fall through the checks and are ignored.
    String json = "{\"jsonrpc\": \"2.0\", \"method\": \"update\", \"params\": [], \"id\": true}";
    JsonRpcRequest req = deserialize(json);

    assertNull(req.getId());
  }
}
