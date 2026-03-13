/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rpc.jsonrpc;

import io.jooby.StatusCode;

/**
 * Standard JSON-RPC 2.0 Error Codes mapped to HTTP status codes.
 *
 * <p>The JSON-RPC 2.0 specification defines a specific set of integer codes for standard errors.
 * This enumeration provides the canonical mapping between those JSON-RPC errors and Jooby's {@link
 * StatusCode} for HTTP transport bindings.
 */
public enum JsonRpcErrorCode {

  // --- Core JSON-RPC 2.0 Errors ---

  /** The JSON sent is not a valid Request object. */
  INVALID_REQUEST(-32600, "Invalid Request", StatusCode.BAD_REQUEST, true),

  /**
   * Invalid JSON was received by the server. An error occurred on the server while parsing the JSON
   * text.
   */
  PARSE_ERROR(-32700, "Parse error", StatusCode.BAD_REQUEST, true),

  /** The method does not exist / is not available. */
  METHOD_NOT_FOUND(-32601, "Method not found", StatusCode.NOT_FOUND, true),

  /** Invalid method parameter(s). */
  INVALID_PARAMS(-32602, "Invalid params", StatusCode.BAD_REQUEST, true),

  /** Internal JSON-RPC error. */
  INTERNAL_ERROR(-32603, "Internal error", StatusCode.SERVER_ERROR, true),

  // --- Implementation-defined Server Errors (-32000 to -32099) ---

  /** Missing or invalid authentication. */
  UNAUTHORIZED(-32001, "Unauthorized", StatusCode.UNAUTHORIZED, false),

  /** Authenticated user lacks required permissions. */
  FORBIDDEN(-32003, "Forbidden", StatusCode.FORBIDDEN, false),

  /** The requested resource or procedure was not found (Business Logic). */
  NOT_FOUND_ERROR(-32004, "Not found", StatusCode.NOT_FOUND, false),

  /** State conflict, such as a duplicate database entry. */
  CONFLICT(-32009, "Conflict", StatusCode.CONFLICT, false),

  /** The client's preconditions were not met. */
  PRECONDITION_FAILED(-32012, "Precondition failed", StatusCode.PRECONDITION_FAILED, false),

  /**
   * The payload format is valid, but the content is semantically incorrect (e.g., validation
   * failed).
   */
  UNPROCESSABLE_CONTENT(-32022, "Unprocessable content", StatusCode.UNPROCESSABLE_ENTITY, false),

  /** Rate limiting applied. */
  TOO_MANY_REQUESTS(-32029, "Too many requests", StatusCode.TOO_MANY_REQUESTS, false);

  private final int code;
  private final String message;
  private final StatusCode statusCode;
  private final boolean protocol;

  /**
   * Defines a JSON-RPC error code mapping.
   *
   * @param code The JSON-RPC 2.0 integer code.
   * @param message The standard error message.
   * @param statusCode The HTTP status code to associate with this error.
   * @param protocol True if this is a strict JSON-RPC 2.0 protocol error, false if
   *     implementation-defined.
   */
  JsonRpcErrorCode(int code, String message, StatusCode statusCode, boolean protocol) {
    this.code = code;
    this.message = message;
    this.statusCode = statusCode;
    this.protocol = protocol;
  }

  /**
   * Retrieves the JSON-RPC integer code.
   *
   * @return The integer code (e.g., -32600).
   */
  public int getCode() {
    return code;
  }

  /**
   * Retrieves the standard JSON-RPC error message.
   *
   * @return The error message.
   */
  public String getMessage() {
    return message;
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
   * Indicates if this error is a core JSON-RPC 2.0 protocol error.
   *
   * @return True for strict protocol errors (-32700 to -32603), false for implementation-defined
   *     errors.
   */
  public boolean isProtocol() {
    return protocol;
  }

  /**
   * Resolves the closest JSON-RPC error code for a given Jooby HTTP status code.
   *
   * <p>If an exact match is not found for the provided HTTP status, this method falls back to
   * {@link #INTERNAL_ERROR}.
   *
   * @param status The Jooby HTTP status code.
   * @return The corresponding {@code JsonRpcErrorCode}, or {@code INTERNAL_ERROR} if no match
   *     exists.
   */
  public static JsonRpcErrorCode of(StatusCode status) {
    for (var errorCode : values()) {
      if (!errorCode.protocol && errorCode.statusCode.value() == status.value()) {
        return errorCode;
      }
    }
    return INTERNAL_ERROR;
  }
}
