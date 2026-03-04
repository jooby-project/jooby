/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import io.jooby.trpc.TrpcDecoder;
import io.jooby.trpc.TrpcReader;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

public class JacksonTrpcReader implements TrpcReader {

  private final JsonParser parser;

  public JacksonTrpcReader(JsonParser parser) {
    this.parser = parser;
    // tRPC multi-args are encoded as a JSON array (Tuple).
    // Ensure the stream starts correctly.
    var token = parser.nextToken();

    if (token != JsonToken.START_ARRAY) {
      throw new IllegalArgumentException("Expected JSON array for tRPC multi-argument payload");
    }
  }

  @Override
  public int nextInt(String name) {
    parser.nextToken();
    return parser.getIntValue();
  }

  @Override
  public long nextLong(String name) {
    parser.nextToken();
    return parser.getLongValue();
  }

  @Override
  public boolean nextBoolean(String name) {
    parser.nextToken();
    return parser.getBooleanValue();
  }

  @Override
  public double nextDouble(String name) {
    parser.nextToken();
    return parser.getDoubleValue();
  }

  @Override
  public String nextString(String name) {
    parser.nextToken();
    return parser.getString();
  }

  @Override
  public <T> T nextObject(String name, TrpcDecoder<T> decoder) {
    parser.nextToken();

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
