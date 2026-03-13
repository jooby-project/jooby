/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rpc.jsonrpc;

/**
 * Exception thrown when a JSON-RPC error occurs during routing, parsing, or execution.
 *
 * <p>Contains standard JSON-RPC 2.0 error codes. When caught by the dispatcher, this exception
 * should be transformed into a {@link JsonRpcResponse} containing the error details.
 */
public class JsonRpcException extends RuntimeException {
  private final JsonRpcErrorCode code;

  private final Object data;

  /**
   * Constructs a new JSON-RPC exception.
   *
   * @param code The integer error code (preferably one of the standard constants).
   * @param message A short description of the error.
   */
  public JsonRpcException(JsonRpcErrorCode code, String message) {
    super(message);
    this.code = code;
    this.data = null;
  }

  /**
   * Constructs a new JSON-RPC exception.
   *
   * @param code The integer error code (preferably one of the standard constants).
   * @param message A short description of the error.
   * @param cause The underlying cause of the error.
   */
  public JsonRpcException(JsonRpcErrorCode code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.data = null;
  }

  /**
   * Constructs a new JSON-RPC exception with additional error data.
   *
   * @param code The integer error code.
   * @param message A short description of the error.
   * @param data Additional data about the error (e.g., stack trace or validation messages).
   */
  public JsonRpcException(JsonRpcErrorCode code, String message, Object data) {
    super(message);
    this.code = code;
    this.data = data;
  }

  /**
   * Returns the JSON-RPC error code.
   *
   * @return The JSON-RPC error code.
   */
  public JsonRpcErrorCode getCode() {
    return code;
  }

  /**
   * Returns additional data regarding the error.
   *
   * @return Additional data regarding the error, or null if none was provided.
   */
  public Object getData() {
    return data;
  }
}
