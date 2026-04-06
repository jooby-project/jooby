/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.jackson2;

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.jsonrpc.JsonRpcDecoder;
import io.jooby.jsonrpc.JsonRpcParser;
import io.jooby.jsonrpc.JsonRpcReader;

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
