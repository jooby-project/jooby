/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.jackson3;

import io.jooby.exception.MissingValueException;
import io.jooby.trpc.TrpcDecoder;
import io.jooby.trpc.TrpcReader;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

public class JacksonTrpcReader implements TrpcReader {
  private final JsonParser parser;
  private boolean hasPeeked = false;
  private final boolean isTuple;
  private boolean isFirstRead = true;

  public JacksonTrpcReader(JsonParser parser, boolean isTuple) {
    this.parser = parser;
    this.isTuple = isTuple;
    var token = parser.nextToken();
    if (isTuple && token != JsonToken.START_ARRAY) {
      throw new IllegalArgumentException("Expected tRPC tuple array");
    }
  }

  @Override
  public boolean nextIsNull(String name) {
    if (!hasPeeked) {
      advance(name);
      hasPeeked = true;
    }

    if (parser.currentToken() == JsonToken.VALUE_NULL) {
      hasPeeked = false; // Consume the null token
      return true;
    }

    return false;
  }

  private void ensureNext(String name) {
    if (hasPeeked) {
      hasPeeked = false;
      return;
    }
    advance(name);
  }

  private void ensureNonNull(String name) {
    if (parser.currentToken() == JsonToken.VALUE_NULL) throw new MissingValueException(name);
  }

  private void advance(String name) {
    // If it's a seamless raw value, we are ALREADY on the token. Do not advance.
    if (!isTuple) {
      if (!isFirstRead) throw new MissingValueException(name);
      isFirstRead = false;
      // The constructor already positioned us on the root token. Do not advance.
      return;
    }

    var token = parser.nextToken();
    if (token == JsonToken.END_ARRAY || token == null) {
      throw new MissingValueException(name);
    }
  }

  @Override
  public int nextInt(String name) {
    ensureNext(name);
    ensureNonNull(name);
    return parser.getIntValue();
  }

  @Override
  public long nextLong(String name) {
    ensureNext(name);
    ensureNonNull(name);
    return parser.getLongValue();
  }

  @Override
  public boolean nextBoolean(String name) {
    ensureNext(name);
    ensureNonNull(name);
    return parser.getBooleanValue();
  }

  @Override
  public double nextDouble(String name) {
    ensureNext(name);
    ensureNonNull(name);
    return parser.getDoubleValue();
  }

  @Override
  public String nextString(String name) {
    ensureNext(name);
    ensureNonNull(name);
    return parser.getString();
  }

  @Override
  public <T> T nextObject(String name, TrpcDecoder<T> decoder) {
    ensureNext(name);
    ensureNonNull(name);
    JacksonTrpcDecoder<T> jacksonDecoder = (JacksonTrpcDecoder<T>) decoder;
    return jacksonDecoder.reader.readValue(parser);
  }

  @Override
  public void close() {
    parser.close();
  }
}
