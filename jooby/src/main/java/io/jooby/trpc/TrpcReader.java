/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import io.jooby.exception.MissingValueException;

/**
 * A sequential reader used at RUNTIME for multi-argument methods. Allows the APT code to extract
 * arguments one by one, preserving primitives.
 */
public interface TrpcReader extends AutoCloseable {

  boolean nextIsNull(String name);

  // 2. Default helper for non-nullable primitives to fail fast
  default void requireNext(String name) {
    if (nextIsNull(name)) {
      throw new MissingValueException(name);
    }
  }

  int nextInt(String name);

  long nextLong(String name);

  boolean nextBoolean(String name);

  double nextDouble(String name);

  String nextString(String name);

  // Object extractor using a pre-resolved decoder
  <T> T nextObject(String name, TrpcDecoder<T> decoder);
}
