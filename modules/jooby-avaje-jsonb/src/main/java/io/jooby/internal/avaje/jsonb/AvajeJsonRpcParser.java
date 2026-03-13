/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import java.lang.reflect.Type;

import io.avaje.jsonb.Jsonb;
import io.jooby.rpc.jsonrpc.JsonRpcDecoder;
import io.jooby.rpc.jsonrpc.JsonRpcParser;
import io.jooby.rpc.jsonrpc.JsonRpcReader;

public class AvajeJsonRpcParser implements JsonRpcParser {

  private final Jsonb jsonb;

  public AvajeJsonRpcParser(Jsonb jsonb) {
    this.jsonb = jsonb;
  }

  @Override
  public <T> JsonRpcDecoder<T> decoder(Type type) {
    return new AvajeJsonRpcDecoder<>(jsonb, type);
  }

  @Override
  public JsonRpcReader reader(Object params) {
    // params will be either a List (positional) or a Map (named)
    return new AvajeJsonRpcReader(params);
  }
}
