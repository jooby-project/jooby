/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import io.avaje.json.JsonReader;
import io.jooby.exception.MissingValueException;
import io.jooby.rpc.trpc.TrpcDecoder;
import io.jooby.rpc.trpc.TrpcReader;

public class AvajeTrpcReader implements TrpcReader {
  private final JsonReader reader;
  private boolean hasPeeked = false;
  private final boolean isTuple;
  private boolean isFirstRead = true;

  public AvajeTrpcReader(JsonReader reader, boolean isTuple) {
    this.reader = reader;
    this.isTuple = isTuple;
    if (isTuple) {
      reader.beginArray();
    }
  }

  @Override
  public boolean nextIsNull(String name) {
    if (!hasPeeked) {
      ensureNextState(name);
      hasPeeked = true;
    }

    if (reader.isNullValue()) {
      reader.skipValue();
      hasPeeked = false;
      return true;
    }

    return false;
  }

  private void ensureNextState(String name) {
    if (isTuple) {
      if (!reader.hasNextElement()) {
        throw new MissingValueException(name);
      }
    } else {
      if (!isFirstRead) {
        throw new MissingValueException(name);
      }
      isFirstRead = false;
    }
  }

  private void ensureNext(String name) {
    if (hasPeeked) {
      hasPeeked = false;
      return;
    }
    ensureNextState(name);
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
    AvajeTrpcDecoder<T> avajeDecoder = (AvajeTrpcDecoder<T>) decoder;
    return avajeDecoder.typeAdapter.fromJson(reader);
  }

  @Override
  public void close() {
    if (isTuple) {
      reader.endArray();
    }
    reader.close();
  }
}
