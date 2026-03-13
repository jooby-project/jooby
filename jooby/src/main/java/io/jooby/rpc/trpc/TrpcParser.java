/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rpc.trpc;

import java.lang.reflect.Type;

/**
 * The core JSON parsing SPI (Service Provider Interface) for tRPC.
 *
 * <p>This factory is implemented by Jooby's JSON modules (such as Jackson, Gson, JSON-B, or Avaje)
 * and serves as the bridge between incoming tRPC network payloads and the application's Java
 * objects.
 *
 * <p><b>Startup Optimization:</b>
 *
 * <p>The Jooby Annotation Processor (APT) generates highly optimized routing code that relies on
 * this interface. At application startup, the generated routes use this parser to eagerly resolve
 * and cache type-specific {@link TrpcDecoder}s. This ensures that during actual HTTP requests,
 * payloads are deserialized with near-zero reflection overhead.
 */
public interface TrpcParser {

  /**
   * Resolves and caches a type-specific deserializer during route initialization.
   *
   * <p>This method is invoked by the APT-generated code when the application starts. By eagerly
   * requesting a decoder for complex generic types, the framework avoids expensive reflection
   * lookups during runtime request processing.
   *
   * @param type The target Java type (e.g., {@code List<Movie>}, {@code User}) to decode.
   * @param <T> The expected Java type.
   * @return A highly optimized decoder capable of parsing network payloads into the target type.
   */
  <T> TrpcDecoder<T> decoder(Type type);

  /**
   * Creates a stateful, sequential reader for extracting tRPC arguments from a raw byte payload.
   *
   * <p>Because tRPC network payloads can either be a single value or a JSON array (tuple) of
   * multiple arguments, this reader manages the cursor position to sequentially hand off the
   * correct JSON segment to the pre-resolved {@link TrpcDecoder}s.
   *
   * @param payload The raw JSON byte array received from the network.
   * @param isTuple {@code true} if the payload is a JSON array representing multiple arguments;
   *     {@code false} if it represents a single, standalone argument.
   * @return A reader for sequential argument extraction.
   */
  TrpcReader reader(byte[] payload, boolean isTuple);

  /**
   * Creates a stateful, sequential reader for extracting tRPC arguments from a string payload.
   *
   * <p>Because tRPC network payloads can either be a single value or a JSON array (tuple) of
   * multiple arguments, this reader manages the cursor position to sequentially hand off the
   * correct JSON segment to the pre-resolved {@link TrpcDecoder}s.
   *
   * @param payload The JSON string received from the network.
   * @param isTuple {@code true} if the payload is a JSON array representing multiple arguments;
   *     {@code false} if it represents a single, standalone argument.
   * @return A reader for sequential argument extraction.
   */
  TrpcReader reader(String payload, boolean isTuple);
}
