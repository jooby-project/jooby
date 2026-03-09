/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.jooby.jsonrpc.JsonRpcDecoder;
import io.jooby.jsonrpc.JsonRpcErrorCode;
import io.jooby.jsonrpc.JsonRpcException;

public class AvajeJsonRpcDecoder<T> implements JsonRpcDecoder<T> {

  private final Jsonb jsonb;
  private final JsonType<T> typeAdapter;

  public AvajeJsonRpcDecoder(Jsonb jsonb, JsonType<T> typeAdapter) {
    this.jsonb = jsonb;
    this.typeAdapter = typeAdapter;
  }

  @Override
  public T decode(String name, Object node) {
    try {
      if (node == null) {
        return null;
      }
      // Convert the Map/List/primitive back to JSON, then to the target type
      // This leverages Avaje's exact mappings without needing a tree traversal model
      String json = jsonb.toJson(node);
      return typeAdapter.fromJson(json);
    } catch (Exception x) {
      throw new JsonRpcException(
          JsonRpcErrorCode.INVALID_PARAMS,
          "Invalid params: unable to map parameter '" + name + "'",
          x);
    }
  }
}
