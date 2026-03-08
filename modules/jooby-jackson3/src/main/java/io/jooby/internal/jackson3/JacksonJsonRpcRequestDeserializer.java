/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import io.jooby.jsonrpc.JsonRpcRequest;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.node.ArrayNode;

public class JacksonJsonRpcRequestDeserializer extends StdDeserializer<JsonRpcRequest> {

  public JacksonJsonRpcRequestDeserializer() {
    super(JsonRpcRequest.class);
  }

  @Override
  public JsonRpcRequest deserialize(JsonParser p, DeserializationContext ctxt) {
    JsonNode node = p.readValueAsTree();

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
      } else if (idNode.isString()) {
        req.setId(idNode.asString());
      }
    }

    // 2. Validate JSON-RPC version
    JsonNode versionNode = node.get("jsonrpc");
    if (versionNode == null || !versionNode.isString() || !"2.0".equals(versionNode.asString())) {
      req.setMethod(null); // Triggers -32600 Invalid Request
      return req;
    }

    // 3. Extract Method
    JsonNode methodNode = node.get("method");
    if (methodNode != null && methodNode.isString()) {
      req.setMethod(methodNode.asString());
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
