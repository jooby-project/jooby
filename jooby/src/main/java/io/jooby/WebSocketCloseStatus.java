/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Collection of websocket close status.
 *
 * @author edgar
 * @since 2.2.0
 */
public class WebSocketCloseStatus {

  /**
   * 1000 indicates a normal closure, meaning that the purpose for which the connection
   * was established has been fulfilled.
   */
  public static final int NORMAL_CODE = 1000;

  /**
   * 1001 indicates that an endpoint is "going away", such as a server going down or a
   * browser having navigated away from a page.
   */
  public static final int GOING_AWAY_CODE = 1001;

  /**
   * 1002 indicates that an endpoint is terminating the connection due to a protocol error.
   */
  public static final int PROTOCOL_ERROR_CODE = 1002;

  /**
   * 1003 indicates that an endpoint is terminating the connection because it has
   * received a type of data it cannot accept (e.g., an endpoint that understands only
   * text data MAY send this if it receives a binary message).
   */
  public static final int NOT_ACCEPTABLE_CODE = 1003;

  /**
   * 1006 indicates that an endpoint is terminating the connection.
   */
  public static final int HARSH_DISCONNECT_CODE = 1006;

  /**
   * 1007 indicates that an endpoint is terminating the connection because it has
   * received data within a message that was not consistent with the type of the message
   * (e.g., non-UTF-8 [RFC3629] data within a text message).
   */
  public static final int BAD_DATA_CODE = 1007;

  /**
   * 1008 indicates that an endpoint is terminating the connection because it has
   * received a message that violates its policy. This is a generic status code that can
   * be returned when there is no other more suitable status code (e.g., 1003 or 1009)
   * or if there is a need to hide specific details about the policy.
   */
  public static final int POLICY_VIOLATION_CODE = 1008;

  /**
   * 1009 indicates that an endpoint is terminating the connection because it has
   * received a message that is too big for it to process.
   */
  public static final int TOO_BIG_TO_PROCESS_CODE = 1009;

  /**
   * 1010 indicates that an endpoint (client) is terminating the connection because it
   * has expected the server to negotiate one or more extension, but the server didn't
   * return them in the response message of the WebSocket handshake. The list of
   * extensions that are needed SHOULD appear in the /reason/ part of the Close frame.
   * Note that this status code is not used by the server, because it can fail the
   * WebSocket handshake instead.
   */
  public static final int REQUIRED_EXTENSION_CODE = 1010;

  /**
   * 1011 indicates that a server is terminating the connection because it encountered
   * an unexpected condition that prevented it from fulfilling the request.
   */
  public static final int SERVER_ERROR_CODE = 1011;

  /**
   * 1012 indicates that the service is restarted. A client may reconnect, and if it
   * chooses to do, should reconnect using a randomized delay of 5 - 30s.
   */
  public static final int SERVICE_RESTARTED_CODE = 1012;

  /**
   * 1013 indicates that the service is experiencing overload. A client should only
   * connect to a different IP (when there are multiple for the target) or reconnect to
   * the same IP upon user action.
   */
  public static final int SERVICE_OVERLOAD_CODE = 1013;

  /**
   * 1000 indicates a normal closure, meaning that the purpose for which the connection
   * was established has been fulfilled.
   */
  public static final WebSocketCloseStatus NORMAL = new WebSocketCloseStatus(NORMAL_CODE, "Normal");

  /**
   * 1001 indicates that an endpoint is "going away", such as a server going down or a
   * browser having navigated away from a page.
   */
  public static final WebSocketCloseStatus GOING_AWAY = new WebSocketCloseStatus(GOING_AWAY_CODE,
      "Going away");

  /**
   * 1002 indicates that an endpoint is terminating the connection due to a protocol
   * error.
   */
  public static final WebSocketCloseStatus PROTOCOL_ERROR = new WebSocketCloseStatus(
      PROTOCOL_ERROR_CODE,
      "Protocol error");

  /**
   * 1003 indicates that an endpoint is terminating the connection because it has
   * received a type of data it cannot accept (e.g., an endpoint that understands only
   * text data MAY send this if it receives a binary message).
   */
  public static final WebSocketCloseStatus NOT_ACCEPTABLE = new WebSocketCloseStatus(
      NOT_ACCEPTABLE_CODE,
      "Not acceptable");

  /** 1006 indicates that an endpoint is terminating the connection. */
  public static final WebSocketCloseStatus HARSH_DISCONNECT = new WebSocketCloseStatus(
      HARSH_DISCONNECT_CODE, "Harsh disconnect");

  /**
   * 1007 indicates that an endpoint is terminating the connection because it has
   * received data within a message that was not consistent with the type of the message
   * (e.g., non-UTF-8 [RFC3629] data within a text message).
   */
  public static final WebSocketCloseStatus BAD_DATA = new WebSocketCloseStatus(BAD_DATA_CODE,
      "Bad data");

  /**
   * 1008 indicates that an endpoint is terminating the connection because it has
   * received a message that violates its policy. This is a generic status code that can
   * be returned when there is no other more suitable status code (e.g., 1003 or 1009)
   * or if there is a need to hide specific details about the policy.
   */
  public static final WebSocketCloseStatus POLICY_VIOLATION = new WebSocketCloseStatus(
      POLICY_VIOLATION_CODE,
      "Policy violation");

  /**
   * 1009 indicates that an endpoint is terminating the connection because it has
   * received a message that is too big for it to process.
   */
  public static final WebSocketCloseStatus TOO_BIG_TO_PROCESS = new WebSocketCloseStatus(
      TOO_BIG_TO_PROCESS_CODE, "Too big to process");

  /**
   * 1010 indicates that an endpoint (client) is terminating the connection because it
   * has expected the server to negotiate one or more extension, but the server didn't
   * return them in the response message of the WebSocket handshake. The list of
   * extensions that are needed SHOULD appear in the /reason/ part of the Close frame.
   * Note that this status code is not used by the server, because it can fail the
   * WebSocket handshake instead.
   */
  public static final WebSocketCloseStatus REQUIRED_EXTENSION = new WebSocketCloseStatus(
      REQUIRED_EXTENSION_CODE, "Required extension");

  /**
   * 1011 indicates that a server is terminating the connection because it encountered
   * an unexpected condition that prevented it from fulfilling the request.
   */
  public static final WebSocketCloseStatus SERVER_ERROR = new WebSocketCloseStatus(
      SERVER_ERROR_CODE,
      "Server error");

  /**
   * 1012 indicates that the service is restarted. A client may reconnect, and if it
   * chooses to do, should reconnect using a randomized delay of 5 - 30s.
   */
  public static final WebSocketCloseStatus SERVICE_RESTARTED = new WebSocketCloseStatus(
      SERVICE_RESTARTED_CODE, "Service restarted");

  /**
   * 1013 indicates that the service is experiencing overload. A client should only
   * connect to a different IP (when there are multiple for the target) or reconnect to
   * the same IP upon user action.
   */
  public static final WebSocketCloseStatus SERVICE_OVERLOAD = new WebSocketCloseStatus(
      SERVICE_OVERLOAD_CODE,
      "Service overload");

  private int code;

  private String reason;

  /**
   * Creates a new websocket close status.
   *
   * @param code Status code.
   * @param reason Reason.
   */
  public WebSocketCloseStatus(int code, @Nullable String reason) {
    this.code = code;
    if (reason != null && reason.length() > 0) {
      this.reason = reason;
    }
  }

  /**
   * Status code.
   *
   * @return Status code.
   */
  public int getCode() {
    return code;
  }

  /**
   * Reason or <code>null</code>.
   *
   * @return Reason or <code>null</code>.
   */
  public @Nullable String getReason() {
    return reason;
  }

  /**
   * Map the status code to one of the existing web socket status.
   *
   * @param code Status code.
   * @return Web socket status or empty.
   */
  public static Optional<WebSocketCloseStatus> valueOf(int code) {
    switch (code) {
      case -1:
      case NORMAL_CODE:
        return Optional.of(NORMAL);
      case GOING_AWAY_CODE:
        return Optional.of(GOING_AWAY);
      case PROTOCOL_ERROR_CODE:
        return Optional.of(PROTOCOL_ERROR);
      case NOT_ACCEPTABLE_CODE:
        return Optional.of(NOT_ACCEPTABLE);
      case BAD_DATA_CODE:
        return Optional.of(BAD_DATA);
      case POLICY_VIOLATION_CODE:
        return Optional.of(POLICY_VIOLATION);
      case TOO_BIG_TO_PROCESS_CODE:
        return Optional.of(TOO_BIG_TO_PROCESS);
      case REQUIRED_EXTENSION_CODE:
        return Optional.of(REQUIRED_EXTENSION);
      case SERVER_ERROR_CODE:
        return Optional.of(SERVER_ERROR);
      case SERVICE_RESTARTED_CODE:
        return Optional.of(SERVICE_RESTARTED);
      case SERVICE_OVERLOAD_CODE:
        return Optional.of(SERVICE_OVERLOAD);
      default:
        return Optional.empty();
    }
  }

  @Override public String toString() {
    StringBuilder buff = new StringBuilder();
    buff.append(code);
    if (reason != null) {
      buff.append("(").append(reason).append(")");
    }
    return buff.toString();
  }
}
