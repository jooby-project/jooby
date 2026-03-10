/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import java.lang.reflect.Type;

import io.jooby.jsonrpc.JsonRpcDecoder;
import io.jooby.jsonrpc.JsonRpcParser;
import io.jooby.jsonrpc.JsonRpcReader;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class JacksonJsonRpcParser implements JsonRpcParser {

  private final ObjectMapper mapper;

  public JacksonJsonRpcParser(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public <T> JsonRpcDecoder<T> decoder(Type type) {
    return new JacksonJsonRpcDecoder<>(mapper, type);
  }

  @Override
  public JsonRpcReader reader(Object params) {
    // The JsonRpcRequestDeserializer stores the params field as a JsonNode
    JsonNode node = (JsonNode) params;
    return new JacksonJsonRpcReader(node);
  }
}
