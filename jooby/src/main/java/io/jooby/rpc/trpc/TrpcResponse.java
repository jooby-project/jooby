/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rpc.trpc;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A standardized envelope for successful tRPC responses.
 *
 * <p>Unlike standard REST endpoints which might return raw JSON objects or arrays directly, the
 * tRPC protocol requires all successful procedure calls to wrap their actual payload inside a
 * specific JSON envelope (specifically, a {@code data} property).
 *
 * <p>This immutable record ensures that the Jooby routing engine serializes the returned Java
 * objects into the exact shape expected by the frontend {@code @trpc/client}, preventing parsing
 * errors on the browser side.
 *
 * @param <T> The type of the underlying data being returned.
 * @param data The actual payload to serialize to the client, or {@code null} if the procedure has
 *     no return value.
 */
public record TrpcResponse<T>(@Nullable T data) {

  /**
   * Wraps a non-null payload into a compliant tRPC success envelope.
   *
   * @param data The actual data to return to the client (e.g., a specific {@code Movie} or {@code
   *     List<Movie>}).
   * @param <T> The type of the data.
   * @return A tRPC response envelope containing the provided data.
   */
  public static @NonNull <T> TrpcResponse<T> of(@NonNull T data) {
    return new TrpcResponse<>(data);
  }

  /**
   * Creates an empty tRPC success envelope.
   *
   * <p>This is typically used by the Jooby routing engine to construct compliant network responses
   * for procedures that return {@code void} or explicitly return no data.
   *
   * @param <T> The inferred type (usually {@code Void} or {@code Object}).
   * @return A tRPC response envelope where the data property is explicitly null.
   */
  public static @NonNull <T> TrpcResponse<T> empty() {
    return new TrpcResponse<>(null);
  }
}
