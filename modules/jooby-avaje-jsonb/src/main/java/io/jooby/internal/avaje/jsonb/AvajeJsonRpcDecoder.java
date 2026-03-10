/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import java.lang.reflect.Type;

import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.jsonrpc.JsonRpcDecoder;

public class AvajeJsonRpcDecoder<T> implements JsonRpcDecoder<T> {

  private final Jsonb jsonb;
  private final Type type;
  private final JsonType<T> typeAdapter;

  public AvajeJsonRpcDecoder(Jsonb jsonb, Type type) {
    this.jsonb = jsonb;
    this.type = type;
    this.typeAdapter = jsonb.type(type);
  }

  @Override
  public T decode(String name, Object node) {
    try {
      if (node == null) {
        throw new MissingValueException(name);
      }
      // Convert the Map/List/primitive back to JSON, then to the target type
      // This leverages Avaje's exact mappings without needing a tree traversal model
      return typeAdapter.fromJson(jsonb.toJson(node));
    } catch (Exception x) {
      throw new TypeMismatchException(name, type, x);
    }
  }
}
