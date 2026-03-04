/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import io.jooby.StatusCode;

public enum TrpcErrorCode {
  BAD_REQUEST(-32600, StatusCode.BAD_REQUEST),
  PARSE_ERROR(-32700, StatusCode.BAD_REQUEST),
  INTERNAL_SERVER_ERROR(-32603, StatusCode.SERVER_ERROR),
  UNAUTHORIZED(-32001, StatusCode.UNAUTHORIZED),
  FORBIDDEN(-32003, StatusCode.FORBIDDEN),
  NOT_FOUND(-32004, StatusCode.NOT_FOUND),
  METHOD_NOT_SUPPORTED(-32005, StatusCode.METHOD_NOT_ALLOWED),
  TIMEOUT(-32008, StatusCode.REQUEST_TIMEOUT),
  CONFLICT(-32009, StatusCode.CONFLICT),
  PRECONDITION_FAILED(-32012, StatusCode.PRECONDITION_FAILED),
  PAYLOAD_TOO_LARGE(-32013, StatusCode.REQUEST_ENTITY_TOO_LARGE),
  UNPROCESSABLE_CONTENT(-32022, StatusCode.UNPROCESSABLE_ENTITY),
  TOO_MANY_REQUESTS(-32029, StatusCode.TOO_MANY_REQUESTS),
  CLIENT_CLOSED_REQUEST(-32099, StatusCode.CLIENT_CLOSED_REQUEST);

  private final int rpcCode;
  private final StatusCode statusCode;

  TrpcErrorCode(int rpcCode, StatusCode statusCode) {
    this.rpcCode = rpcCode;
    this.statusCode = statusCode;
  }

  public int getRpcCode() {
    return rpcCode;
  }

  public StatusCode getStatusCode() {
    return statusCode;
  }

  /** Helper to map a standard Jooby HTTP status code to the closest tRPC equivalent. */
  public static TrpcErrorCode of(StatusCode status) {
    for (var code : values()) {
      if (code.statusCode.value() == status.value()) {
        return code;
      }
    }
    return INTERNAL_SERVER_ERROR; // Fallback
  }
}
