/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public record TrpcResponse<T>(@Nullable T data) {

  public static @NonNull <T> TrpcResponse<T> of(@NonNull T data) {
    return new TrpcResponse<>(data);
  }

  public static @NonNull <T> TrpcResponse<T> empty() {
    return new TrpcResponse<>(null);
  }
}
