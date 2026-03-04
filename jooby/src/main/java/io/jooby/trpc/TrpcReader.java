/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

/**
 * A sequential reader used at RUNTIME for multi-argument methods. Allows the APT code to extract
 * arguments one by one, preserving primitives.
 */
public interface TrpcReader extends AutoCloseable {

  // Primitive extractors to avoid autoboxing
  int nextInt(String name);

  long nextLong(String name);

  boolean nextBoolean(String name);

  double nextDouble(String name);

  String nextString(String name);

  // Object extractor using a pre-resolved decoder
  <T> T nextObject(String name, TrpcDecoder<T> decoder);
}
