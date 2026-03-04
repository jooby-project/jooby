/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.jooby.StatusCode;

public class TrpcException extends RuntimeException {
  private final String procedure;
  private final TrpcErrorCode errorCode;

  public TrpcException(String procedure, StatusCode code, Throwable cause) {
    this(procedure, TrpcErrorCode.of(code), cause);
  }

  public TrpcException(String procedure, TrpcErrorCode code, Throwable cause) {
    super(procedure + ": " + code.name(), cause);
    this.procedure = procedure;
    this.errorCode = code;
  }

  public TrpcException(String procedure, TrpcErrorCode code) {
    super(procedure + ": " + code.name());
    this.procedure = procedure;
    this.errorCode = code;
  }

  public TrpcException(String procedure, StatusCode code) {
    this(procedure, TrpcErrorCode.of(code));
  }

  public StatusCode getStatusCode() {
    return errorCode.getStatusCode();
  }

  public String getProcedure() {
    return procedure;
  }

  public Map<String, Object> toMap() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("code", errorCode.name());
    data.put("httpStatus", errorCode.getStatusCode().value());
    data.put("path", procedure);

    Map<String, Object> error = new LinkedHashMap<>();
    error.put(
        "message",
        Optional.ofNullable(getCause())
            .map(Throwable::getMessage)
            .filter(Objects::nonNull)
            .orElse(errorCode.name()));
    error.put("code", errorCode.getRpcCode());
    error.put("data", data);
    return Map.of("error", error);
  }
}
