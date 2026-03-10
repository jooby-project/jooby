/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

/**
 * Represents a JSON-RPC 2.0 Response object.
 *
 * <p>When an RPC call is made, the Server MUST reply with a Response, except in the case of
 * Notifications. The Response is expressed as a single JSON Object.
 */
public class JsonRpcResponse {

  private String jsonrpc = "2.0";
  private Object result;
  private ErrorDetail error;
  private Object id;

  public JsonRpcResponse() {}

  private JsonRpcResponse(Object id, Object result, ErrorDetail error) {
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
  public static JsonRpcResponse error(Object id, JsonRpcErrorCode code, Object data) {
    return new JsonRpcResponse(
        id, null, new ErrorDetail(code.getCode(), code.getMessage(), data(data)));
  }

  private static Object data(Object data) {
    if (data instanceof Throwable cause) {
      return cause.getMessage();
    }
    return data;
  }

  public String getJsonrpc() {
    return jsonrpc;
  }

  public void setJsonrpc(String jsonrpc) {
    this.jsonrpc = jsonrpc;
  }

  public Object getResult() {
    return result;
  }

  public void setResult(Object result) {
    this.result = result;
  }

  public ErrorDetail getError() {
    return error;
  }

  public void setError(ErrorDetail error) {
    this.error = error;
  }

  public Object getId() {
    return id;
  }

  public void setId(Object id) {
    this.id = id;
  }

  /** Represents the error object inside a JSON-RPC response. */
  public static class ErrorDetail {
    private int code;
    private String message;
    private Object data;

    public ErrorDetail() {}

    public ErrorDetail(int code, String message, Object data) {
      this.code = code;
      this.message = message;
      this.data = data;
    }

    public int getCode() {
      return code;
    }

    public void setCode(int code) {
      this.code = code;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public Object getData() {
      return data;
    }

    public void setData(Object data) {
      this.data = data;
    }
  }
}
