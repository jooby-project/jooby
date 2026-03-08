/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import java.lang.reflect.Type;

/**
 * The core JSON parsing SPI (Service Provider Interface) for JSON-RPC.
 *
 * <p>This factory is implemented by Jooby's JSON modules (such as Jackson or Avaje) and serves as
 * the bridge between the parsed JSON-RPC request and the application's Java objects.
 *
 * <p><b>Startup Optimization:</b>
 *
 * <p>The Jooby Annotation Processor (APT) generates highly optimized routing code that relies on
 * this interface. At application startup, the generated routes use this parser to eagerly resolve
 * and cache type-specific {@link JsonRpcDecoder}s. This ensures that during actual HTTP requests,
 * arguments are deserialized with near-zero reflection overhead.
 */
public interface JsonRpcParser {

  /**
   * Resolves and caches a type-specific deserializer during route initialization.
   *
   * <p>This method is invoked by the APT-generated code when the application starts. By eagerly
   * requesting a decoder for complex generic types, the framework avoids expensive reflection
   * lookups during runtime request processing.
   *
   * @param type The target Java type (e.g., {@code List<Movie>}, {@code User}) to decode.
   * @param <T> The expected Java type.
   * @return A highly optimized decoder capable of parsing generic JSON nodes into the target type.
   */
  <T> JsonRpcDecoder<T> decoder(Type type);

  /**
   * Creates a stateful reader for extracting JSON-RPC arguments from the request parameters.
   *
   * <p>Because JSON-RPC 2.0 parameters can be either positional (a JSON Array) or named (a JSON
   * Object), this reader manages the extraction context to hand off the correct JSON segment to the
   * pre-resolved {@link JsonRpcDecoder}s or extract primitives.
   *
   * @param params The parsed parameter object (typically a List or Map depending on the underlying
   *     JSON library) from the {@link JsonRpcRequest}.
   * @return A reader for argument extraction.
   */
  JsonRpcReader reader(Object params);
}
