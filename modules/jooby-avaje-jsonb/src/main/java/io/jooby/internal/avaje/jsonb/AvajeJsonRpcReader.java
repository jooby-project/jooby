/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import java.util.List;
import java.util.Map;

import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.rpc.jsonrpc.JsonRpcDecoder;
import io.jooby.rpc.jsonrpc.JsonRpcReader;

public class AvajeJsonRpcReader implements JsonRpcReader {

  private final Map<String, Object> map;
  private final List<Object> list;
  private int index = 0;

  @SuppressWarnings("unchecked")
  public AvajeJsonRpcReader(Object params) {
    if (params instanceof List) {
      this.list = (List<Object>) params;
      this.map = null;
    } else if (params instanceof Map) {
      this.map = (Map<String, Object>) params;
      this.list = null;
    } else {
      this.map = null;
      this.list = null;
    }
  }

  private Object peek(String name) {
    if (list != null && index < list.size()) {
      return list.get(index);
    } else if (map != null) {
      return map.get(name);
    }
    return null;
  }

  private Object consume(String name) {
    if (list != null && index < list.size()) {
      return list.get(index++);
    } else if (map != null) {
      return map.get(name);
    }
    return null;
  }

  private Object require(String name) {
    Object value = consume(name);
    if (value == null) {
      throw new MissingValueException(name);
    }
    return value;
  }

  @Override
  public boolean nextIsNull(String name) {
    return peek(name) == null;
  }

  @Override
  public int nextInt(String name) {
    Object val = require(name);
    if (val instanceof Number n) return n.intValue();
    throw new TypeMismatchException(name, int.class);
  }

  @Override
  public long nextLong(String name) {
    Object val = require(name);
    if (val instanceof Number n) return n.longValue();
    throw new TypeMismatchException(name, long.class);
  }

  @Override
  public boolean nextBoolean(String name) {
    Object val = require(name);
    if (val instanceof Boolean b) return b;
    throw new TypeMismatchException(name, boolean.class);
  }

  @Override
  public double nextDouble(String name) {
    Object val = require(name);
    if (val instanceof Number n) return n.doubleValue();
    throw new TypeMismatchException(name, double.class);
  }

  @Override
  public String nextString(String name) {
    return require(name).toString();
  }

  @Override
  public <T> T nextObject(String name, JsonRpcDecoder<T> decoder) {
    var val = require(name);
    return decoder.decode(name, val);
  }

  @Override
  public void close() {}
}
