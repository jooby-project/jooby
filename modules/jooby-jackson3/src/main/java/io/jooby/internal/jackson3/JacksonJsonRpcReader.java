/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import io.jooby.jsonrpc.JsonRpcDecoder;
import io.jooby.jsonrpc.JsonRpcReader;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

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
      return ((ArrayNode) params).get(index);
    } else if (params.isObject()) {
      return ((ObjectNode) params).get(name);
    }
    return null;
  }

  private JsonNode consumeNode(String name) {
    if (params == null) {
      return null;
    }
    if (isArray) {
      return ((ArrayNode) params).get(index++);
    } else if (params.isObject()) {
      return ((ObjectNode) params).get(name);
    }
    return null;
  }

  @Override
  public boolean nextIsNull(String name) {
    JsonNode node = peekNode(name);
    return node == null || node.isNull();
  }

  @Override
  public int nextInt(String name) {
    JsonNode node = consumeNode(name);
    return node == null || node.isNull() ? 0 : node.asInt();
  }

  @Override
  public long nextLong(String name) {
    JsonNode node = consumeNode(name);
    return node == null || node.isNull() ? 0L : node.asLong();
  }

  @Override
  public boolean nextBoolean(String name) {
    JsonNode node = consumeNode(name);
    return node != null && !node.isNull() && node.asBoolean();
  }

  @Override
  public double nextDouble(String name) {
    JsonNode node = consumeNode(name);
    return node == null || node.isNull() ? 0.0 : node.asDouble();
  }

  @Override
  public String nextString(String name) {
    JsonNode node = consumeNode(name);
    return node == null || node.isNull() ? null : node.asText();
  }

  @Override
  public <T> T nextObject(String name, JsonRpcDecoder<T> decoder) {
    JsonNode node = consumeNode(name);
    return decoder.decode(name, node);
  }

  @Override
  public void close() {
    // No network resources to release for an in-memory JsonNode tree
  }
}
