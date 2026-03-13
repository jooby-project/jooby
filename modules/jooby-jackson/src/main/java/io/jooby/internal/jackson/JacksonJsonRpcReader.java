/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.rpc.jsonrpc.JsonRpcDecoder;
import io.jooby.rpc.jsonrpc.JsonRpcReader;

public class JacksonJsonRpcReader implements JsonRpcReader {

  private final JsonNode params;
  private final boolean isArray;
  private int index = 0;

  public JacksonJsonRpcReader(JsonNode params) {
    this.params = params;
    this.isArray = params != null && params.isArray();
  }

  private JsonNode peekNode(String name) {
    if (params == null) {
      return null;
    }
    if (isArray) {
      return params.get(index);
    } else if (params.isObject()) {
      return params.get(name);
    }
    return null;
  }

  private JsonNode consumeNode(String name) {
    if (params == null) {
      return null;
    }
    if (isArray) {
      return params.get(index++);
    } else if (params.isObject()) {
      return params.get(name);
    }
    return null;
  }

  private JsonNode requireNode(String name) {
    JsonNode node = consumeNode(name);
    if (node == null || node.isNull() || node.isMissingNode()) {
      throw new MissingValueException(name);
    }
    return node;
  }

  @Override
  public boolean nextIsNull(String name) {
    JsonNode node = peekNode(name);
    return node == null || node.isNull();
  }

  @Override
  public int nextInt(String name) {
    var node = requireNode(name);
    if (node.isNumber()) {
      return node.intValue();
    }
    throw new TypeMismatchException(name, int.class);
  }

  @Override
  public long nextLong(String name) {
    var node = requireNode(name);
    if (node.isNumber()) {
      return node.longValue();
    }
    throw new TypeMismatchException(name, long.class);
  }

  @Override
  public boolean nextBoolean(String name) {
    var node = requireNode(name);
    if (node.isBoolean()) {
      return node.booleanValue();
    }
    throw new TypeMismatchException(name, boolean.class);
  }

  @Override
  public double nextDouble(String name) {
    var node = requireNode(name);
    if (node.isNumber()) {
      return node.doubleValue();
    }
    throw new TypeMismatchException(name, double.class);
  }

  @Override
  public String nextString(String name) {
    var node = requireNode(name);
    if (node.isTextual()) {
      return node.textValue();
    }
    throw new TypeMismatchException(name, String.class);
  }

  @Override
  public <T> T nextObject(String name, JsonRpcDecoder<T> decoder) {
    var node = requireNode(name);
    return decoder.decode(name, node);
  }

  @Override
  public void close() {}
}
