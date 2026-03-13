/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson;

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.rpc.jsonrpc.JsonRpcDecoder;

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
      if (node == null || ((JsonNode) node).isNull() || ((JsonNode) node).isMissingNode()) {
        throw new MissingValueException(name);
      }
      return mapper.treeToValue((JsonNode) node, javaType);
    } catch (Exception x) {
      throw new TypeMismatchException(name, javaType, x);
    }
  }
}
