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
 *
 * <h3>Exception and Error Handling</h3>
 *
 * <p>When an error or exception occurs during the processing of a JSON-RPC request, it is captured
 * and wrapped within the {@link ErrorDetail} of this response object.
 *
 * <p><strong>Important:</strong> Not all exceptions are converted into an errored response sent to
 * the client. Specifically, if an exception occurs while processing a <strong>Notification</strong>
 * (a request intentionally omitting the {@code id} member), the server MUST NOT reply.
 *
 * <p>In all cases, whether the request is a standard Method Call or a Notification, the exception
 * will <strong>always be logged</strong> by the server infrastructure. However, it is only
 * serialized and transmitted back to the client if the original request required a response.
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
   * @return A populated JsonRpcResponse containing the result.
   */
  public static JsonRpcResponse success(Object id, Object result) {
    return new JsonRpcResponse(id, result, null);
  }

  /**
   * Creates an error JSON-RPC response holding generic data or a Throwable.
   *
   * @param id The id from the corresponding request (or null if a Parse Error / Invalid Request).
   * @param code The JSON-RPC error code.
   * @param data Additional data about the error. If this is a Throwable, it delegates to the
   *     Throwable handler.
   * @return A populated JsonRpcResponse containing the error details.
   */
  public static JsonRpcResponse error(@Nullable Object id, JsonRpcErrorCode code, Object data) {
    if (data instanceof Throwable) {
      return error(id, code, (Throwable) data);
    }
    return new JsonRpcResponse(id, null, new ErrorDetail(code, data));
  }

  /**
   * Creates an error JSON-RPC response originating from a Throwable.
   *
   * @param id The id from the corresponding request (or null if a Parse Error / Invalid Request).
   * @param code The JSON-RPC error code.
   * @param cause The underlying exception that caused the error.
   * @return A populated JsonRpcResponse containing the error details.
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

  /**
   * Represents the error object inside a JSON-RPC response.
   *
   * <p>If constructed with a {@link Throwable}, the throwable is retained for internal server
   * logging via {@link #exception()}, but only its message is exposed to the client payload via
   * {@link #getData()} to prevent leaking sensitive stack traces.
   */
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

    /**
     * Gets the additional error data. If the underlying data is an Exception, this safely returns
     * only the exception message rather than the full stack trace object.
     *
     * @return The error data, or the exception message.
     */
    public @Nullable Object getData() {
      if (data instanceof Throwable cause) {
        return cause.getMessage();
      }
      return data;
    }

    /**
     * Retrieves the raw exception for internal server logging, if one exists.
     *
     * @return The underlying Throwable, or null if the error wasn't caused by an exception.
     */
    public @Nullable Throwable exception() {
      if (data instanceof Throwable cause) {
        return cause;
      }
      return null;
    }
  }
}
