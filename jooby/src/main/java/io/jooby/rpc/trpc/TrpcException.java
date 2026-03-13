/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rpc.trpc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.jooby.StatusCode;

/**
 * A specialized runtime exception that encapsulates a tRPC error.
 *
 * <p>This exception is thrown when a tRPC procedure fails, either due to framework-level issues
 * (like invalid JSON parsing or missing arguments) or user-thrown domain errors. It holds all the
 * necessary context—the procedure name, the specific {@link TrpcErrorCode}, and the underlying
 * cause—required to construct a compliant tRPC error response.
 *
 * <p>The frontend {@code @trpc/client} relies on a very specific JSON envelope to correctly
 * reconstruct the {@code TRPCClientError} on the client side. The {@link #toMap()} method handles
 * translating this Java exception into that exact nested map structure.
 */
public class TrpcException extends RuntimeException {
  private final String procedure;
  private final TrpcErrorCode errorCode;

  /**
   * Constructs a new tRPC exception using a standard Jooby HTTP status code.
   *
   * @param procedure The name of the tRPC procedure that failed (e.g., "movies.getById").
   * @param code The Jooby HTTP status code, which will be mapped to its closest tRPC equivalent.
   * @param cause The underlying exception that caused this failure.
   */
  public TrpcException(String procedure, StatusCode code, Throwable cause) {
    this(procedure, TrpcErrorCode.of(code), cause);
  }

  /**
   * Constructs a new tRPC exception using a specific tRPC error code.
   *
   * @param procedure The name of the tRPC procedure that failed.
   * @param code The explicit tRPC error code.
   * @param cause The underlying exception that caused this failure.
   */
  public TrpcException(String procedure, TrpcErrorCode code, Throwable cause) {
    super(procedure + ": " + code.name(), cause);
    this.procedure = procedure;
    this.errorCode = code;
  }

  /**
   * Constructs a new tRPC exception without an underlying cause.
   *
   * @param procedure The name of the tRPC procedure that failed.
   * @param code The explicit tRPC error code.
   */
  public TrpcException(String procedure, TrpcErrorCode code) {
    super(procedure + ": " + code.name());
    this.procedure = procedure;
    this.errorCode = code;
  }

  /**
   * Constructs a new tRPC exception using a standard Jooby HTTP status code without an underlying
   * cause.
   *
   * @param procedure The name of the tRPC procedure that failed.
   * @param code The Jooby HTTP status code.
   */
  public TrpcException(String procedure, StatusCode code) {
    this(procedure, TrpcErrorCode.of(code));
  }

  /**
   * Gets the HTTP status code associated with this error.
   *
   * @return The Jooby {@link StatusCode} to be set in the HTTP response headers.
   */
  public StatusCode getStatusCode() {
    return errorCode.getStatusCode();
  }

  /**
   * Gets the name of the tRPC procedure that threw this exception.
   *
   * @return The procedure name (e.g., "movies.getById").
   */
  public String getProcedure() {
    return procedure;
  }

  /**
   * Serializes the exception into the exact JSON structure expected by the tRPC protocol.
   *
   * <p>The resulting map translates into the following JSON shape:
   *
   * <pre>{@code
   * {
   * "error": {
   * "message": "The descriptive error message",
   * "code": -32600,
   * "data": {
   * "code": "BAD_REQUEST",
   * "httpStatus": 400,
   * "path": "movies.getById"
   * }
   * }
   * }
   * }</pre>
   *
   * @return A nested map representing the JSON error envelope.
   */
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
