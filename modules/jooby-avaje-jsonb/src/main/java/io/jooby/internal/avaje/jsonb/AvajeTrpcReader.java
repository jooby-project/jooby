/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import io.avaje.json.JsonReader;
import io.jooby.exception.MissingValueException;
import io.jooby.trpc.TrpcDecoder;
import io.jooby.trpc.TrpcReader;

public class AvajeTrpcReader implements TrpcReader {
  private final JsonReader reader;
  private boolean hasPeeked = false;

  public AvajeTrpcReader(JsonReader reader) {
    this.reader = reader;
    reader.beginArray();
  }

  @Override
  public boolean nextIsNull(String name) {
    if (!hasPeeked) {
      if (!reader.hasNextElement()) {
        throw new MissingValueException(name);
      }
      hasPeeked = true; // We successfully advanced the cursor to a value
    }

    if (reader.isNullValue()) {
      // Avaje requires us to actively skip the null token to consume it
      reader.skipValue();
      hasPeeked = false; // Reset because the value is consumed
      return true;
    }

    // It's not null. We leave hasPeeked = true so the next extraction method doesn't advance again.
    return false;
  }

  private void ensureNext(String name) {
    if (hasPeeked) {
      // We already advanced the stream during nextIsNull().
      // Reset the flag since the caller is about to consume the value.
      hasPeeked = false;
      return;
    }

    // hasNextElement() checks for ']' and consumes the comma ',' if present.
    if (!reader.hasNextElement()) {
      throw new MissingValueException(name);
    }
  }

  private void ensureNonNull(String name) {
    if (reader.isNullValue()) throw new MissingValueException(name);
  }

  @Override
  public int nextInt(String name) {
    ensureNext(name);
    ensureNonNull(name);
    return reader.readInt();
  }

  @Override
  public long nextLong(String name) {
    ensureNext(name);
    ensureNonNull(name);
    return reader.readLong();
  }

  @Override
  public boolean nextBoolean(String name) {
    ensureNext(name);
    ensureNonNull(name);
    return reader.readBoolean();
  }

  @Override
  public double nextDouble(String name) {
    ensureNext(name);
    ensureNonNull(name);
    return reader.readDouble();
  }

  @Override
  public String nextString(String name) {
    ensureNext(name);
    ensureNonNull(name);
    return reader.readString();
  }

  @Override
  public <T> T nextObject(String name, TrpcDecoder<T> decoder) {
    ensureNext(name);
    ensureNonNull(name);
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
