/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.avaje.jsonb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.jooby.jsonrpc.JsonRpcRequest;

@ExtendWith(MockitoExtension.class)
class AvajeJsonRpcRequestAdapterTest {

  @Mock Jsonb jsonb;
  @Mock JsonType<Object> anyType;
  @Mock JsonReader reader;
  @Mock JsonWriter writer;

  private AvajeJsonRpcRequestAdapter adapter;

  @BeforeEach
  void setup() {
    when(jsonb.type(Object.class)).thenReturn(anyType);
    adapter = new AvajeJsonRpcRequestAdapter(jsonb);
  }

  // --- BATCH PARSING TESTS ---

  @Test
  void testFromJson_EmptyArray_ReturnsInvalidRequestFlag() {
    when(anyType.fromJson(reader)).thenReturn(Collections.emptyList());

    JsonRpcRequest req = adapter.fromJson(reader);

    assertNull(req.getMethod());
    assertNull(req.getJsonrpc());
    assertFalse(req.isBatch());
  }

  @Test
  void testFromJson_PopulatedArray_ReturnsBatchRequest() {
    Map<String, Object> req1 = new HashMap<>();
    req1.put("jsonrpc", "2.0");
    req1.put("method", "sum");
    req1.put("params", List.of(1, 2));

    Map<String, Object> req2 = new HashMap<>(); // Invalid Request (missing jsonrpc)

    when(anyType.fromJson(reader)).thenReturn(List.of(req1, req2));

    JsonRpcRequest batchReq = adapter.fromJson(reader);

    assertTrue(batchReq.isBatch());
    assertEquals(2, batchReq.getRequests().size());

    // Verify first request
    assertEquals("2.0", batchReq.getRequests().get(0).getJsonrpc());
    assertEquals("sum", batchReq.getRequests().get(0).getMethod());

    // Verify second request (Invalid)
    assertNull(batchReq.getRequests().get(1).getMethod());
  }

  // --- SINGLE PARSING TESTS (Object validation) ---

  @Test
  void testParseSingle_NotAMap_ReturnsInvalidRequest() {
    when(anyType.fromJson(reader)).thenReturn("Just a raw string payload");

    JsonRpcRequest req = adapter.fromJson(reader);

    assertNull(req.getMethod());
  }

  @Test
  void testParseSingle_ValidRequest_WithNumericIdAndListParams() {
    Map<String, Object> map = new HashMap<>();
    map.put("jsonrpc", "2.0");
    map.put("method", "subtract");
    map.put("id", 42); // Numeric ID
    map.put("params", List.of(1, 2, 3)); // List Params

    when(anyType.fromJson(reader)).thenReturn(map);

    JsonRpcRequest req = adapter.fromJson(reader);

    assertEquals("2.0", req.getJsonrpc());
    assertEquals("subtract", req.getMethod());
    assertEquals(42, req.getId());
    assertTrue(req.getParams() instanceof List);
  }

  @Test
  void testParseSingle_ValidRequest_WithStringIdAndMapParams() {
    Map<String, Object> map = new HashMap<>();
    map.put("jsonrpc", "2.0");
    map.put("method", "update");
    map.put("id", "req-100"); // String ID
    map.put("params", Map.of("key", "val")); // Map Params

    when(anyType.fromJson(reader)).thenReturn(map);

    JsonRpcRequest req = adapter.fromJson(reader);

    assertEquals("2.0", req.getJsonrpc());
    assertEquals("update", req.getMethod());
    assertEquals("req-100", req.getId());
    assertTrue(req.getParams() instanceof Map);
  }

  @Test
  void testParseSingle_ValidNotification_NoId() {
    Map<String, Object> map = new HashMap<>();
    map.put("jsonrpc", "2.0");
    map.put("method", "notify");

    when(anyType.fromJson(reader)).thenReturn(map);

    JsonRpcRequest req = adapter.fromJson(reader);

    assertEquals("notify", req.getMethod());
    assertNull(req.getId());
    assertNull(req.getParams()); // Null params are allowed
  }

  // --- ERROR SCENARIOS (Triggers Invalid Request via null method) ---

  @Test
  void testParseSingle_InvalidVersion_ReturnsInvalidRequest() {
    Map<String, Object> map = new HashMap<>();
    map.put("jsonrpc", "1.0"); // Wrong version
    map.put("method", "test");

    when(anyType.fromJson(reader)).thenReturn(map);

    JsonRpcRequest req = adapter.fromJson(reader);
    assertNull(req.getMethod());
  }

  @Test
  void testParseSingle_MissingMethod_ReturnsInvalidRequest() {
    Map<String, Object> map = new HashMap<>();
    map.put("jsonrpc", "2.0");
    // Missing method

    when(anyType.fromJson(reader)).thenReturn(map);

    JsonRpcRequest req = adapter.fromJson(reader);
    assertNull(req.getMethod());
  }

  @Test
  void testParseSingle_MethodIsNotAString_ReturnsInvalidRequest() {
    Map<String, Object> map = new HashMap<>();
    map.put("jsonrpc", "2.0");
    map.put("method", 12345); // Invalid method type

    when(anyType.fromJson(reader)).thenReturn(map);

    JsonRpcRequest req = adapter.fromJson(reader);
    assertNull(req.getMethod());
  }

  @Test
  void testParseSingle_InvalidParamsType_ReturnsInvalidRequest() {
    Map<String, Object> map = new HashMap<>();
    map.put("jsonrpc", "2.0");
    map.put("method", "test");
    map.put("params", "primitive string params are invalid"); // Must be List or Map

    when(anyType.fromJson(reader)).thenReturn(map);

    JsonRpcRequest req = adapter.fromJson(reader);
    assertNull(req.getMethod());
  }

  // --- ID EXTRACTION COVERAGE (Edge Cases) ---

  @Test
  void testParseSingle_IdIsBoolean_Ignored() {
    Map<String, Object> map = new HashMap<>();
    map.put("jsonrpc", "2.0");
    map.put("method", "test");
    map.put("id", true); // Boolean IDs are not spec-compliant, should be ignored

    when(anyType.fromJson(reader)).thenReturn(map);

    JsonRpcRequest req = adapter.fromJson(reader);

    assertEquals("test", req.getMethod()); // Request is still valid
    assertNull(req.getId()); // But ID is safely ignored
  }

  // --- SERIALIZATION TESTS ---

  @Test
  void testToJson_ThrowsUnsupportedOperationException() {
    UnsupportedOperationException ex =
        assertThrows(
            UnsupportedOperationException.class,
            () -> adapter.toJson(writer, new JsonRpcRequest()));

    assertEquals("Serialization of JsonRpcRequest is not required", ex.getMessage());
  }
}
