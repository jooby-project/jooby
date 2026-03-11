/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import io.jooby.StatusCode;

/**
 * Maps standard Jooby HTTP status codes to official tRPC error codes.
 *
 * <p>The tRPC specification dictates that failed requests must return specific JSON-RPC style
 * integer codes (e.g., -32600) in the JSON response payload, alongside the standard HTTP status
 * codes. This enumeration defines the canonical mapping between Jooby's {@link StatusCode} and the
 * expected tRPC error shape.
 *
 * <p>When an exception is thrown within a tRPC route, Jooby uses this mapping to format the error
 * response so the frontend {@code @trpc/client} can correctly parse and reconstruct the {@code
 * TRPCClientError}.
 */
public enum TrpcErrorCode {

  /** Invalid routing or parameters. Mapped to HTTP 400. */
  BAD_REQUEST(-32600, StatusCode.BAD_REQUEST),

  /** The server received invalid JSON. Mapped to HTTP 400. */
  PARSE_ERROR(-32700, StatusCode.BAD_REQUEST),

  /** Internal server error. Mapped to HTTP 500. */
  INTERNAL_SERVER_ERROR(-32603, StatusCode.SERVER_ERROR),

  /** Missing or invalid authentication. Mapped to HTTP 401. */
  UNAUTHORIZED(-32001, StatusCode.UNAUTHORIZED),

  /** Authenticated user lacks required permissions. Mapped to HTTP 403. */
  FORBIDDEN(-32003, StatusCode.FORBIDDEN),

  /** The requested resource or tRPC procedure was not found. Mapped to HTTP 404. */
  NOT_FOUND(-32004, StatusCode.NOT_FOUND),

  /**
   * The HTTP method used is not supported by the procedure (e.g., GET on a mutation). Mapped to
   * HTTP 405.
   */
  METHOD_NOT_SUPPORTED(-32005, StatusCode.METHOD_NOT_ALLOWED),

  /** The request took too long to process. Mapped to HTTP 408. */
  TIMEOUT(-32008, StatusCode.REQUEST_TIMEOUT),

  /** State conflict, such as a duplicate database entry. Mapped to HTTP 409. */
  CONFLICT(-32009, StatusCode.CONFLICT),

  /** The client's preconditions were not met. Mapped to HTTP 412. */
  PRECONDITION_FAILED(-32012, StatusCode.PRECONDITION_FAILED),

  /** The incoming request payload exceeds the allowed limits. Mapped to HTTP 413. */
  PAYLOAD_TOO_LARGE(-32013, StatusCode.REQUEST_ENTITY_TOO_LARGE),

  /** The payload format is valid, but the content is semantically incorrect. Mapped to HTTP 422. */
  UNPROCESSABLE_CONTENT(-32022, StatusCode.UNPROCESSABLE_ENTITY),

  /** Rate limiting applied. Mapped to HTTP 429. */
  TOO_MANY_REQUESTS(-32029, StatusCode.TOO_MANY_REQUESTS),

  /** The client disconnected before the server could respond. Mapped to HTTP 499. */
  CLIENT_CLOSED_REQUEST(-32099, StatusCode.CLIENT_CLOSED_REQUEST);

  private final int rpcCode;
  private final StatusCode statusCode;

  /**
   * Defines a tRPC error code mapping.
   *
   * @param rpcCode The JSON-RPC 2.0 compatible integer code specified by tRPC.
   * @param statusCode The HTTP status code to return in the response headers.
   */
  TrpcErrorCode(int rpcCode, StatusCode statusCode) {
    this.rpcCode = rpcCode;
    this.statusCode = statusCode;
  }

  /**
   * Retrieves the JSON-RPC style integer code.
   *
   * @return The tRPC integer code (e.g., -32600).
   */
  public int getRpcCode() {
    return rpcCode;
  }

  /**
   * Retrieves the corresponding HTTP status code.
   *
   * @return The Jooby {@link StatusCode}.
   */
  public StatusCode getStatusCode() {
    return statusCode;
  }

  /**
   * Resolves the closest tRPC error code for a given Jooby HTTP status code.
   *
   * <p>If an exact match is not found for the provided HTTP status, this method falls back to
   * {@link #INTERNAL_SERVER_ERROR}.
   *
   * @param status The Jooby HTTP status code.
   * @return The corresponding {@code TrpcErrorCode}, or {@code INTERNAL_SERVER_ERROR} if no match
   *     exists.
   */
  public static TrpcErrorCode of(StatusCode status) {
    for (var code : values()) {
      if (code.statusCode.value() == status.value()) {
        return code;
      }
    }
    return INTERNAL_SERVER_ERROR; // Fallback
  }
}
