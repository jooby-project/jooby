/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rpc.trpc;

/**
 * A pre-resolved decoder used at runtime to deserialize tRPC network payloads into complex Java
 * objects.
 *
 * <p>This interface is heavily utilized by the Jooby Annotation Processor (APT). When compiling
 * tRPC-annotated controllers, the APT generates highly optimized routing code that resolves the
 * appropriate {@code TrpcDecoder} for each method argument. By pre-resolving these decoders, Jooby
 * efficiently parses incoming JSON payloads without incurring reflection overhead on every request.
 *
 * <p>Note: Primitive types and standard wrappers (like {@code int}, {@code String}, {@code
 * boolean}) are typically handled directly by the {@code TrpcReader} rather than requiring a
 * dedicated decoder.
 *
 * @param <T> The target Java type this decoder produces.
 */
public interface TrpcDecoder<T> {

  /**
   * Decodes a raw byte array payload into the target Java object.
   *
   * @param name The name of the parameter being decoded (useful for error reporting or wrapping).
   * @param payload The raw JSON byte array received from the network.
   * @return The fully deserialized Java object.
   */
  T decode(String name, byte[] payload);

  /**
   * Decodes a string payload into the target Java object.
   *
   * @param name The name of the parameter being decoded (useful for error reporting or wrapping).
   * @param payload The JSON string received from the network.
   * @return The fully deserialized Java object.
   */
  T decode(String name, String payload);
}
