/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.jackson3;

import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.jsonrpc.JsonRpcDecoder;
import io.jooby.jsonrpc.JsonRpcReader;
import tools.jackson.databind.JsonNode;

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
    if (node.isInt()) {
      return node.asInt();
    }
    throw new TypeMismatchException(name, int.class);
  }

  @Override
  public long nextLong(String name) {
    var node = requireNode(name);
    if (node.isLong()) {
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
    if (node.isDouble()) {
      return node.asDouble();
    }
    throw new TypeMismatchException(name, double.class);
  }

  @Override
  public String nextString(String name) {
    var node = requireNode(name);
    if (node.isString()) {
      return node.asString();
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
