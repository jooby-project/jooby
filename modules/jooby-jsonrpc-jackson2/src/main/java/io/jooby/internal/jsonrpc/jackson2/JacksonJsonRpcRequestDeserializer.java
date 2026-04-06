/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.jackson2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.jooby.jsonrpc.JsonRpcRequest;

public class JacksonJsonRpcRequestDeserializer extends StdDeserializer<JsonRpcRequest> {

  public JacksonJsonRpcRequestDeserializer() {
    super(JsonRpcRequest.class);
  }

  @Override
  public JsonRpcRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    // Jackson 2 standard way to read the tree
    JsonNode node = p.getCodec().readTree(p);

    if (node.isArray()) {
      var arrayNode = (ArrayNode) node;

      // Spec: Empty array must return a single Invalid Request object (-32600)
      if (arrayNode.isEmpty()) {
        JsonRpcRequest invalid = new JsonRpcRequest();
        invalid.setMethod(null); // Acts as a flag for Invalid Request
        invalid.setBatch(false); // Force single return shape
        return invalid;
      }

      JsonRpcRequest batch = new JsonRpcRequest();
      for (JsonNode element : arrayNode) {
        batch.add(parseSingle(element));
      }
      return batch;
    } else {
      return parseSingle(node);
    }
  }

  private JsonRpcRequest parseSingle(JsonNode node) {
    var req = new JsonRpcRequest();

    if (!node.isObject()) {
      req.setMethod(null);
      return req;
    }

    // 1. Extract ID if present (crucial for error echoing)
    JsonNode idNode = node.get("id");
    if (idNode != null && !idNode.isNull()) {
      if (idNode.isNumber()) {
        req.setId(idNode.numberValue());
      } else if (idNode.isTextual()) { // Jackson 2 uses isTextual()
        req.setId(idNode.asText()); // Jackson 2 uses asText() instead of asString()
      }
    }

    // 2. Validate JSON-RPC version
    JsonNode versionNode = node.get("jsonrpc");
    if (versionNode == null || !versionNode.isTextual() || !"2.0".equals(versionNode.asText())) {
      req.setMethod(null); // Triggers -32600 Invalid Request
      return req;
    }

    // 3. Extract Method
    JsonNode methodNode = node.get("method");
    if (methodNode != null && methodNode.isTextual()) {
      req.setMethod(methodNode.asText());
    } else {
      req.setMethod(null); // Triggers -32600 Invalid Request
    }

    // 4. Extract Params (Must be an Array or an Object per spec)
    JsonNode paramsNode = node.get("params");
    if (paramsNode != null && !paramsNode.isNull()) {
      if (paramsNode.isArray() || paramsNode.isObject()) {
        req.setParams(paramsNode); // Keep as JsonNode for the Reader later
      } else {
        req.setMethod(null); // Primitive params are invalid -> -32600
      }
    }

    return req;
  }
}
