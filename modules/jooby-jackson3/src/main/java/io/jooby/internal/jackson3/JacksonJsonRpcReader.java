/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import io.jooby.exception.MissingValueException;
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
    return requireNode(name).asInt();
  }

  @Override
  public long nextLong(String name) {
    return requireNode(name).asLong();
  }

  @Override
  public boolean nextBoolean(String name) {
    return requireNode(name).asBoolean();
  }

  @Override
  public double nextDouble(String name) {
    return requireNode(name).asDouble();
  }

  @Override
  public String nextString(String name) {
    return requireNode(name).asText();
  }

  @Override
  public <T> T nextObject(String name, JsonRpcDecoder<T> decoder) {
    JsonNode node = requireNode(name);
    return decoder.decode(name, node);
  }

  @Override
  public void close() {
    // No network resources to release for an in-memory JsonNode tree
  }
}
