/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import java.lang.reflect.Type;

import io.jooby.jsonrpc.JsonRpcDecoder;
import io.jooby.jsonrpc.JsonRpcException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class JacksonJsonRpcDecoder<T> implements JsonRpcDecoder<T> {

  private final ObjectMapper mapper;
  private final JavaType javaType;

  public JacksonJsonRpcDecoder(ObjectMapper mapper, Type type) {
    this.mapper = mapper;
    this.javaType = mapper.constructType(type);
  }

  @Override
  public T decode(String name, Object node) {
    try {
      if (node == null || ((JsonNode) node).isNull()) {
        return null;
      }
      return mapper.treeToValue((JsonNode) node, javaType);
    } catch (IllegalArgumentException x) {
      throw new JsonRpcException(
          -32602, "Invalid params: unable to map parameter '" + name + "'", x.getMessage());
    } catch (Exception x) {
      throw new JsonRpcException(
          -32602, "Invalid params: unable to map parameter '" + name + "'", x.toString());
    }
  }
}
