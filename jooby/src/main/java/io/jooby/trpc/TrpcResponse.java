/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

public record TrpcResponse<T>(TrpcResult<T> result) {

  public static <T> TrpcResponse<T> success(T data) {
    return new TrpcResponse<>(new TrpcResult<>(data));
  }
}
