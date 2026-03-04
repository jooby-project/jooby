/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import io.avaje.json.JsonReader;
import io.jooby.trpc.TrpcDecoder;
import io.jooby.trpc.TrpcReader;

public class AvajeTrpcReader implements TrpcReader {

  private final JsonReader reader;

  public AvajeTrpcReader(JsonReader reader) {
    this.reader = reader;
    reader.beginArray();
  }

  private void ensureNext() {
    // hasNextElement() checks for ']' and consumes the comma ',' if present.
    if (!reader.hasNextElement()) {
      throw new IllegalArgumentException("Not enough arguments in tRPC tuple payload");
    }
  }

  @Override
  public int nextInt(String name) {
    ensureNext();
    return reader.readInt();
  }

  @Override
  public long nextLong(String name) {
    ensureNext();
    return reader.readLong();
  }

  @Override
  public boolean nextBoolean(String name) {
    ensureNext();
    return reader.readBoolean();
  }

  @Override
  public double nextDouble(String name) {
    ensureNext();
    return reader.readDouble();
  }

  @Override
  public String nextString(String name) {
    ensureNext();
    return reader.readString();
  }

  @Override
  public <T> T nextObject(String name, TrpcDecoder<T> decoder) {
    ensureNext();
    // Cast to access the underlying Avaje JsonType adapter
    AvajeTrpcDecoder<T> avajeDecoder = (AvajeTrpcDecoder<T>) decoder;

    // JsonType.fromJson(JsonReader) consumes exactly the tokens needed
    // for the object, leaving the stream in the correct position.
    return avajeDecoder.typeAdapter.fromJson(reader);
  }

  @Override
  public void close() {
    // Consume the closing ']' and close the underlying stream
    reader.endArray();
    reader.close();
  }
}
