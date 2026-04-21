/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * Represents a JSON-RPC 2.0 Response object.
 *
 * <p>When an RPC call is made, the Server MUST reply with a Response, except in the case of
 * Notifications. The Response is expressed as a single JSON Object.
 */
public class JsonRpcResponse {

  private String jsonrpc = "2.0";
  private @Nullable Object result;
  private @Nullable ErrorDetail error;
  private @Nullable Object id;

  private JsonRpcResponse(
      @Nullable Object id, @Nullable Object result, @Nullable ErrorDetail error) {
    this.id = id;
    this.result = result;
    this.error = error;
  }

  /**
   * Creates a successful JSON-RPC response.
   *
   * @param id The id from the corresponding request.
   * @param result The result of the invoked method.
   * @return A populated JsonRpcResponse.
   */
  public static JsonRpcResponse success(Object id, Object result) {
    return new JsonRpcResponse(id, result, null);
  }

  /**
   * Creates an error JSON-RPC response.
   *
   * @param id The id from the corresponding request.
   * @param code The error code.
   * @param data Additional data about the error.
   * @return A populated JsonRpcResponse.
   */
  public static JsonRpcResponse error(@Nullable Object id, JsonRpcErrorCode code, Object data) {
    if (data instanceof Throwable) {
      return error(id, code, (Throwable) data);
    }
    return new JsonRpcResponse(id, null, new ErrorDetail(code, data));
  }

  /**
   * Creates an error JSON-RPC response.
   *
   * @param id The id from the corresponding request.
   * @param code The error code.
   * @param cause Additional data about the error.
   * @return A populated JsonRpcResponse.
   */
  public static JsonRpcResponse error(
      @Nullable Object id, JsonRpcErrorCode code, @Nullable Throwable cause) {
    return new JsonRpcResponse(id, null, new ErrorDetail(code, cause));
  }

  public String getJsonrpc() {
    return jsonrpc;
  }

  public void setJsonrpc(String jsonrpc) {
    this.jsonrpc = jsonrpc;
  }

  public @Nullable Object getResult() {
    return result;
  }

  public void setResult(@Nullable Object result) {
    this.result = result;
  }

  public @Nullable ErrorDetail getError() {
    return error;
  }

  public void setError(@Nullable ErrorDetail error) {
    this.error = error;
  }

  public @Nullable Object getId() {
    return id;
  }

  public void setId(@Nullable Object id) {
    this.id = id;
  }

  /** Represents the error object inside a JSON-RPC response. */
  public static class ErrorDetail {
    private final int code;
    private final String message;
    private final @Nullable Object data;

    public ErrorDetail(JsonRpcErrorCode code, @Nullable String message, @Nullable Object data) {
      this.code = code.getCode();
      this.message = Optional.ofNullable(message).orElse(code.getMessage());
      this.data = data;
    }

    public ErrorDetail(JsonRpcErrorCode code, @Nullable Object data) {
      this(code, null, data);
    }

    public ErrorDetail(JsonRpcErrorCode code) {
      this(code, null, null);
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public @Nullable Object getData() {
      if (data instanceof Throwable cause) {
        return cause.getMessage();
      }
      return data;
    }

    public @Nullable Throwable exception() {
      if (data instanceof Throwable cause) {
        return cause;
      }
      return null;
    }
  }
}
