/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

/** A pre-resolved decoder used at RUNTIME for single-argument methods. */
public interface TrpcDecoder<T> {
  T decode(String name, byte[] payload);

  T decode(String name, String payload);
}
