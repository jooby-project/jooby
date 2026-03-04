/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import io.jooby.exception.MissingValueException;
import io.jooby.trpc.TrpcDecoder;
import io.jooby.trpc.TrpcReader;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

public class JacksonTrpcReader implements TrpcReader {
  private final JsonParser parser;
  private boolean hasPeeked = false;

  public JacksonTrpcReader(JsonParser parser) {
    this.parser = parser;
    parser.nextToken();
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

    // It's not null. We leave hasPeeked = true so extraction doesn't advance again.
    return false;
  }

  private void ensureNext(String name) {
    if (hasPeeked) {
      // We already advanced the stream during nextIsNull().
      // Reset the flag since the caller is about to consume the value.
      hasPeeked = false;
      return;
    }

    advance(name);
  }

  private void ensureNonNull(String name) {
    if (parser.currentToken() == JsonToken.VALUE_NULL) throw new MissingValueException(name);
  }

  private void advance(String name) {
    JsonToken token = parser.nextToken();
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

    // Cast back to our specific implementation to access the underlying ObjectReader.
    // This allows us to read complex objects directly from the current position
    // in the stream without any intermediate byte[] buffering or allocation.
    JacksonTrpcDecoder<T> jacksonDecoder = (JacksonTrpcDecoder<T>) decoder;
    return jacksonDecoder.reader.readValue(parser);
  }

  @Override
  public void close() {
    parser.close();
  }
}
