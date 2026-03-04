/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import java.lang.reflect.Type;

/**
 * Factory provided by the JSON module (Jackson or Avaje). Used by the APT-generated routes AT
 * STARTUP to cache deserializers.
 */
public interface TrpcParser {

  /** Resolves and caches the deserializer for a specific type during route initialization. */
  <T> TrpcDecoder<T> decoder(Type type);

  /**
   * Creates a sequential reader for parsing tRPC arguments from a JSON array.
   *
   * @param payload A JSON array containing the method arguments.
   * @return A reader for sequential argument extraction.
   */
  TrpcReader reader(byte[] payload);

  /**
   * Creates a sequential reader for parsing tRPC arguments from a JSON array.
   *
   * @param payload A JSON array containing the method arguments.
   * @return A reader for sequential argument extraction.
   */
  TrpcReader reader(String payload);
}
