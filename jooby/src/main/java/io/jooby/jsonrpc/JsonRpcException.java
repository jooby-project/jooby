/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

/**
 * Exception thrown when a JSON-RPC error occurs during routing, parsing, or execution.
 *
 * <p>Contains standard JSON-RPC 2.0 error codes. When caught by the dispatcher, this exception
 * should be transformed into a {@link JsonRpcResponse} containing the error details.
 */
public class JsonRpcException extends RuntimeException {

  /**
   * Invalid JSON was received by the server. An error occurred on the server while parsing the JSON
   * text.
   */
  public static final int PARSE_ERROR = -32700;

  /** The JSON sent is not a valid Request object. */
  public static final int INVALID_REQUEST = -32600;

  /** The method does not exist / is not available. */
  public static final int METHOD_NOT_FOUND = -32601;

  /** Invalid method parameter(s). */
  public static final int INVALID_PARAMS = -32602;

  /** Internal JSON-RPC error. */
  public static final int INTERNAL_ERROR = -32603;

  private final int code;
  private final Object data;

  /**
   * Constructs a new JSON-RPC exception.
   *
   * @param code The integer error code (preferably one of the standard constants).
   * @param message A short description of the error.
   */
  public JsonRpcException(int code, String message) {
    super(message);
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
  public JsonRpcException(int code, String message, Object data) {
    super(message);
    this.code = code;
    this.data = data;
  }

  /**
   * Returns the JSON-RPC error code.
   *
   * @return The JSON-RPC error code.
   */
  public int getCode() {
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
