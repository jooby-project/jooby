/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rpc.jsonrpc;

/**
 * A pre-resolved decoder used at runtime to deserialize JSON-RPC parameter nodes into complex Java
 * objects.
 *
 * <p>This interface is heavily utilized by the Jooby Annotation Processor (APT). When compiling
 * {@code @JsonRpc}-annotated controllers, the APT generates highly optimized routing code that
 * resolves the appropriate {@code JsonRpcDecoder} for each method argument. By pre-resolving these
 * decoders, Jooby efficiently parses incoming JSON arguments without incurring reflection overhead
 * on every request.
 *
 * <p>Note: Primitive types and standard wrappers (like {@code int}, {@code String}, {@code
 * boolean}) are typically handled directly by the {@link JsonRpcReader} rather than requiring a
 * dedicated decoder.
 *
 * @param <T> The target Java type this decoder produces.
 */
public interface JsonRpcDecoder<T> {

  /**
   * Decodes a generic JSON node object into the target Java object.
   *
   * @param name The name of the parameter being decoded (useful for error reporting or wrapping).
   * @param node The generic JSON node (e.g., a Map, List, or library-specific AST node)
   *     representing this specific argument.
   * @return The fully deserialized Java object.
   */
  T decode(String name, Object node);
}
