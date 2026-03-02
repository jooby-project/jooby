/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import java.util.Map;

public record TrpcError(ErrorDetail error) {
  public record ErrorDetail(String message, int code, Map<String, Object> data) {}
}
