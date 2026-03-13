/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import java.util.List;
import java.util.Map;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.jooby.rpc.jsonrpc.JsonRpcRequest;

public class AvajeJsonRpcRequestAdapter implements JsonAdapter<JsonRpcRequest> {

  private final JsonType<Object> anyType;

  public AvajeJsonRpcRequestAdapter(Jsonb jsonb) {
    // The Object.class adapter parses JSON arrays to Lists and objects to Maps
    this.anyType = jsonb.type(Object.class);
  }

  @Override
  public JsonRpcRequest fromJson(JsonReader reader) {
    Object payload = anyType.fromJson(reader);

    if (payload instanceof List<?> list) {
      // Spec: Empty array must return a single Invalid Request object (-32600)
      if (list.isEmpty()) {
        JsonRpcRequest invalid = new JsonRpcRequest();
        invalid.setMethod(null);
        invalid.setBatch(false);
        return invalid;
      }

      JsonRpcRequest batch = new JsonRpcRequest();
      for (Object element : list) {
        batch.add(parseSingle(element));
      }
      return batch;
    } else {
      return parseSingle(payload);
    }
  }

  private JsonRpcRequest parseSingle(Object node) {
    JsonRpcRequest req = new JsonRpcRequest();

    if (!(node instanceof Map<?, ?> map)) {
      req.setMethod(null); // Triggers -32600 Invalid Request
      return req;
    }

    // 1. Extract ID
    Object idVal = map.get("id");
    if (idVal != null) {
      if (idVal instanceof Number n) {
        req.setId(n);
      } else if (idVal instanceof String s) {
        req.setId(s);
      }
    }

    // 2. Validate JSON-RPC version
    Object versionVal = map.get("jsonrpc");
    if (!"2.0".equals(versionVal)) {
      req.setMethod(null);
      return req;
    }

    // 3. Extract Method
    Object methodVal = map.get("method");
    if (methodVal instanceof String s) {
      req.setMethod(s);
    } else {
      req.setMethod(null);
    }

    // 4. Extract Params (Must be Map or List per spec)
    Object paramsVal = map.get("params");
    if (paramsVal != null) {
      if (paramsVal instanceof Map || paramsVal instanceof List) {
        req.setParams(paramsVal);
      } else {
        req.setMethod(null); // Primitive params -> -32600
      }
    }

    return req;
  }

  @Override
  public void toJson(JsonWriter writer, JsonRpcRequest value) {
    // We only deserialize inbound requests, servers don't emit them
    throw new UnsupportedOperationException("Serialization of JsonRpcRequest is not required");
  }
}
