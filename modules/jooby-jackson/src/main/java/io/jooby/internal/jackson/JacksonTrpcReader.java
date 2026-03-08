/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.jooby.SneakyThrows;
import io.jooby.exception.MissingValueException;
import io.jooby.trpc.TrpcDecoder;
import io.jooby.trpc.TrpcReader;

public class JacksonTrpcReader implements TrpcReader {
  private final JsonParser parser;
  private boolean hasPeeked = false;
  private final boolean isTuple;
  private boolean isFirstRead = true;

  public JacksonTrpcReader(JsonParser parser, boolean isTuple) {
    this.parser = parser;
    this.isTuple = isTuple;
    var token = nextToken();
    if (isTuple && token != JsonToken.START_ARRAY) {
      throw new IllegalArgumentException("Expected tRPC tuple array");
    }
  }

  private JsonToken nextToken() {
    try {
      return parser.nextToken();
    } catch (IOException e) {
      throw SneakyThrows.propagate(e);
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

    var token = nextToken();
    if (token == JsonToken.END_ARRAY || token == null) {
      throw new MissingValueException(name);
    }
  }

  @Override
  public int nextInt(String name) {
    ensureNext(name);
    ensureNonNull(name);
    try {
      return parser.getIntValue();
    } catch (IOException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  @Override
  public long nextLong(String name) {
    ensureNext(name);
    ensureNonNull(name);
    try {
      return parser.getLongValue();
    } catch (IOException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  @Override
  public boolean nextBoolean(String name) {
    ensureNext(name);
    ensureNonNull(name);
    try {
      return parser.getBooleanValue();
    } catch (IOException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  @Override
  public double nextDouble(String name) {
    ensureNext(name);
    ensureNonNull(name);
    try {
      return parser.getDoubleValue();
    } catch (IOException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  @Override
  public String nextString(String name) {
    ensureNext(name);
    ensureNonNull(name);
    try {
      return parser.getText();
    } catch (IOException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  @Override
  public <T> T nextObject(String name, TrpcDecoder<T> decoder) {
    try {
      ensureNext(name);
      ensureNonNull(name);
      JacksonTrpcDecoder<T> jacksonDecoder = (JacksonTrpcDecoder<T>) decoder;
      return jacksonDecoder.reader.readValue(parser);
    } catch (IOException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  @Override
  public void close() throws Exception {
    parser.close();
  }
}
