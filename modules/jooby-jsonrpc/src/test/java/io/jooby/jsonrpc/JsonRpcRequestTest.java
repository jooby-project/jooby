/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

class JsonRpcRequestTest {

  @Test
  void testConstants() {
    assertNotNull(JsonRpcRequest.BAD_REQUEST);
    assertEquals("2.0", JsonRpcRequest.JSONRPC);
  }

  @Test
  void testIsValid() {
    JsonRpcRequest req = new JsonRpcRequest();

    // 1. Both null
    assertFalse(req.isValid());

    // 2. Correct JSONRPC, but method is null
    req.setJsonrpc("2.0");
    assertFalse(req.isValid());

    // 3. Correct JSONRPC, method is empty
    req.setMethod("");
    assertFalse(req.isValid());

    // 4. Correct JSONRPC, method is blank (whitespace only)
    req.setMethod("   ");
    assertFalse(req.isValid());

    // 5. Correct JSONRPC, valid method
    req.setMethod("myMethod");
    assertTrue(req.isValid());

    // 6. Invalid JSONRPC version, valid method
    req.setJsonrpc("1.0");
    assertFalse(req.isValid());
  }

  @Test
  void testGettersAndSetters() {
    JsonRpcRequest req = new JsonRpcRequest();

    req.setJsonrpc("2.0");
    assertEquals("2.0", req.getJsonrpc());

    req.setMethod("testMethod");
    assertEquals("testMethod", req.getMethod());

    Object params = new Object();
    req.setParams(params);
    assertEquals(params, req.getParams());

    req.setId(12345);
    assertEquals(12345, req.getId());

    req.setBatch(true);
    assertTrue(req.isBatch());
  }

  @Test
  void testBatchStateManagement() {
    JsonRpcRequest req = new JsonRpcRequest();

    // Initially not a batch and requests list is empty
    assertFalse(req.isBatch());
    assertTrue(req.getRequests().isEmpty());

    // Add first request (triggers internal array initialization and batch=true)
    JsonRpcRequest child1 = new JsonRpcRequest();
    req.add(child1);

    assertTrue(req.isBatch());
    assertEquals(1, req.getRequests().size());
    assertSame(child1, req.getRequests().get(0));

    // Add second request (uses existing array)
    JsonRpcRequest child2 = new JsonRpcRequest();
    req.add(child2);

    assertEquals(2, req.getRequests().size());
    assertSame(child2, req.getRequests().get(1));
  }

  @Test
  void testSetRequestsBulk() {
    JsonRpcRequest req = new JsonRpcRequest();
    List<JsonRpcRequest> children = List.of(new JsonRpcRequest(), new JsonRpcRequest());

    req.setRequests(children);

    assertTrue(req.isBatch());
    assertSame(children, req.getRequests());
  }

  @Test
  void testIteratorForSingleRequest() {
    JsonRpcRequest req = new JsonRpcRequest();
    req.setMethod("singleMethod");

    // Because it's not a batch, it should yield exactly one element (itself)
    Iterator<JsonRpcRequest> iterator = req.iterator();

    assertTrue(iterator.hasNext());
    assertSame(req, iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  void testIteratorForBatchRequest() {
    JsonRpcRequest req = new JsonRpcRequest();
    JsonRpcRequest child1 = new JsonRpcRequest();
    JsonRpcRequest child2 = new JsonRpcRequest();

    req.add(child1).add(child2);

    // Because it's a batch, it should yield the children
    Iterator<JsonRpcRequest> iterator = req.iterator();

    assertTrue(iterator.hasNext());
    assertSame(child1, iterator.next());

    assertTrue(iterator.hasNext());
    assertSame(child2, iterator.next());

    assertFalse(iterator.hasNext());
  }
}
