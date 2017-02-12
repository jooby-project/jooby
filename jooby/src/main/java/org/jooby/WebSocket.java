/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jooby.internal.RouteMatcher;
import org.jooby.internal.RoutePattern;
import org.jooby.internal.WebSocketImpl;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * <h1>WebSockets</h1>
 * <p>
 * Creating web sockets is pretty straightforward:
 * </p>
 *
 * <pre>
 *  {
 *    ws("/", (ws) {@literal ->} {
 *      // connected
 *      ws.onMessage(message {@literal ->} {
 *        System.out.println(message.value());
 *        ws.send("Message Received");
 *      });
 *      ws.send("Connected");
 *    });
 *  }
 * </pre>
 *
 * First thing you need to do is to register a new web socket in your App using the
 * {@link Jooby#ws(String, WebSocket.Handler)} method.
 * You can specify a path to listen for web socket connection. The path can be a static path or
 * a path pattern (like routes).
 *
 * On new connections the {@link WebSocket.Handler#connect(WebSocket)} will be executed from there
 * you can listen on {@link #onMessage(Callback)}, {@link #onClose(Callback)} or
 * {@link #onError(ErrCallback)} events.
 *
 * Inside a handler you can send text or binary message.
 *
 * <h2>Data types</h2>
 * <p>
 * If your web socket is suppose to send/received a specific data type, like:
 * <code>json</code> it is nice to define a consumes and produces types:
 * </p>
 *
 * <pre>
 *   ws("/", (ws) {@literal ->} {
 *     ws.onMessage(message {@literal ->} {
 *       // read as json
 *       MyObject object = message.to(MyObject.class);
 *     });
 *
 *     MyObject object = new MyObject();
 *     ws.send(object); // convert to text message using a json converter
 *   })
 *   .consumes(MediaType.json)
 *   .produces(MediaType.json);
 * </pre>
 *
 *
 * @author edgar
 * @since 0.1.0
 */
public interface WebSocket extends Closeable, Registry {

  /** Websocket key. */
  Key<Set<WebSocket.Definition>> KEY = Key.get(new TypeLiteral<Set<WebSocket.Definition>>() {
  });

  /**
   * A web socket connect handler. Executed every time a new client connect to the socket.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface FullHandler {
    /**
     * Inside a connect event, you can listen for {@link WebSocket#onMessage(Callback)},
     * {@link WebSocket#onClose(Callback)} or {@link WebSocket#onError(ErrCallback)} events.
     *
     * Also, you can send text and binary message.
     *
     * @param req Current request.
     * @param ws A web socket.
     * @throws Exception If something goes wrong while connecting.
     */
    void connect(Request req, WebSocket ws) throws Exception;
  }

  /**
   * A web socket connect handler. Executed every time a new client connect to the socket.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Handler extends FullHandler {

    @Override
    default void connect(final Request req, final WebSocket ws) throws Exception {
      connect(ws);
    }

    /**
     * Inside a connect event, you can listen for {@link WebSocket#onMessage(Callback)},
     * {@link WebSocket#onClose(Callback)} or {@link WebSocket#onError(ErrCallback)} events.
     *
     * Also, you can send text and binary message.
     *
     * @param ws A web socket.
     * @throws Exception If something goes wrong while connecting.
     */
    void connect(WebSocket ws) throws Exception;
  }

  /**
   * Hold a status code and optionally a reason message for {@link WebSocket#close()} operations.
   *
   * @author edgar
   * @since 0.1.0
   */
  class CloseStatus {
    /** A status code. */
    private final int code;

    /** A close reason. */
    private final String reason;

    /**
     * Create a new {@link CloseStatus} instance.
     *
     * @param code the status code
     */
    private CloseStatus(final int code) {
      this(code, null);
    }

    /**
     * Create a new {@link CloseStatus} instance.
     *
     * @param code the status code
     * @param reason the reason
     */
    private CloseStatus(final int code, final String reason) {
      Preconditions.checkArgument((code >= 1000 && code < 5000), "Invalid code: %s", code);
      this.code = code;
      this.reason = reason == null || reason.isEmpty() ? null : reason;
    }

    /**
     * Creates a new {@link CloseStatus}.
     *
     * @param code A status code.
     * @return A new close status.
     */
    public static CloseStatus of(final int code) {
      return new CloseStatus(code);
    }

    /**
     * Creates a new {@link CloseStatus}.
     *
     * @param code A status code.
     * @param reason A close reason.
     * @return A new close status.
     */
    public static CloseStatus of(final int code, final String reason) {
      requireNonNull(reason, "A reason is required.");
      return new CloseStatus(code, reason);
    }

    /**
     * @return the status code.
     */
    public int code() {
      return this.code;
    }

    /**
     * @return the reason or {@code null}.
     */
    public String reason() {
      return this.reason;
    }

    @Override
    public String toString() {
      if (reason == null) {
        return code + "";
      }
      return code + " (" + reason + ")";
    }
  }

  /**
   * Web socket callback.
   *
   * @author edgar
   * @since 0.1.0
   * @param <T> Param type.
   */
  interface Callback<T> {

    /**
     * Invoked from a web socket.
     *
     * @param data A data argument.
     * @throws Exception If something goes wrong.
     */
    void invoke(T data) throws Exception;
  }

  /**
   * Web socket success callback.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface SuccessCallback {

    /**
     * Invoked from a web socket.
     *
     * @throws Exception If something goes wrong.
     */
    void invoke() throws Exception;
  }

  /**
   * Web socket err callback.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface ErrCallback {

    /**
     * Invoked if something goes wrong.
     *
     * @param err Err cause.
     */
    void invoke(Throwable err);
  }

  /**
   * Configure a web socket.
   *
   * @author edgar
   * @since 0.1.0
   */
  class Definition {
    /**
     * A route compiled pattern.
     */
    private RoutePattern routePattern;

    /**
     * Defines the media types that the methods of a resource class or can consumes. Default is:
     * {@literal *}/{@literal *}.
     */
    private MediaType consumes = MediaType.plain;

    /**
     * Defines the media types that the methods of a resource class or can produces. Default is:
     * {@literal *}/{@literal *}.
     */
    private MediaType produces = MediaType.plain;

    /** A path pattern. */
    private String pattern;

    /** A ws handler. */
    private FullHandler handler;

    /**
     * Creates a new {@link Definition}.
     *
     * @param pattern A path pattern.
     * @param handler A ws handler.
     */
    public Definition(final String pattern, final FullHandler handler) {
      requireNonNull(pattern, "A route path is required.");
      requireNonNull(handler, "A handler is required.");

      this.routePattern = new RoutePattern("WS", pattern);
      // normalized pattern
      this.pattern = routePattern.pattern();
      this.handler = handler;
    }

    /**
     * @return A route pattern.
     */
    public String pattern() {
      return pattern;
    }

    /**
     * Test if the given path matches this web socket.
     *
     * @param path A path pattern.
     * @return A web socket or empty optional.
     */
    public Optional<WebSocket> matches(final String path) {
      RouteMatcher matcher = routePattern.matcher("WS" + path);
      if (matcher.matches()) {
        return Optional.of(asWebSocket(matcher));
      }
      return Optional.empty();
    }

    /**
     * Set the media types the route can consume.
     *
     * @param type The media types to test for.
     * @return This route definition.
     */
    public Definition consumes(final String type) {
      return consumes(MediaType.valueOf(type));
    }

    /**
     * Set the media types the route can consume.
     *
     * @param type The media types to test for.
     * @return This route definition.
     */
    public Definition consumes(final MediaType type) {
      this.consumes = requireNonNull(type, "A type is required.");
      return this;
    }

    /**
     * Set the media types the route can produces.
     *
     * @param type The media types to test for.
     * @return This route definition.
     */
    public Definition produces(final String type) {
      return produces(MediaType.valueOf(type));
    }

    /**
     * Set the media types the route can produces.
     *
     * @param type The media types to test for.
     * @return This route definition.
     */
    public Definition produces(final MediaType type) {
      this.produces = requireNonNull(type, "A type is required.");
      return this;
    }

    /**
     * @return All the types this route can consumes.
     */
    public MediaType consumes() {
      return this.consumes;
    }

    /**
     * @return All the types this route can produces.
     */
    public MediaType produces() {
      return this.produces;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof Definition) {
        Definition def = (Definition) obj;
        return this.pattern.equals(def.pattern);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return pattern.hashCode();
    }

    @Override
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      buffer.append("WS ").append(pattern()).append("\n");
      buffer.append("  consume: ").append(consumes()).append("\n");
      buffer.append("  produces: ").append(produces()).append("\n");
      return buffer.toString();
    }

    /**
     * Creates a new web socket.
     *
     * @param matcher A route matcher.
     * @return A new web socket.
     */
    private WebSocket asWebSocket(final RouteMatcher matcher) {
      return new WebSocketImpl(handler, matcher.path(), pattern, matcher.vars(),
          consumes, produces);
    }
  }

  /** Default success callback. */
  SuccessCallback SUCCESS = () -> {
  };

  /** Default err callback. */
  ErrCallback ERR = (ex) -> {
    LoggerFactory.getLogger(WebSocket.class).error("error while sending data", ex);
  };

  /**
   * "1000 indicates a normal closure, meaning that the purpose for which the connection
   * was established has been fulfilled."
   */
  CloseStatus NORMAL = new CloseStatus(1000, "Normal");

  /**
   * "1001 indicates that an endpoint is "going away", such as a server going down or a
   * browser having navigated away from a page."
   */
  CloseStatus GOING_AWAY = new CloseStatus(1001, "Going away");

  /**
   * "1002 indicates that an endpoint is terminating the connection due to a protocol
   * error."
   */
  CloseStatus PROTOCOL_ERROR = new CloseStatus(1002, "Protocol error");

  /**
   * "1003 indicates that an endpoint is terminating the connection because it has
   * received a type of data it cannot accept (e.g., an endpoint that understands only
   * text data MAY send this if it receives a binary message)."
   */
  CloseStatus NOT_ACCEPTABLE = new CloseStatus(1003, "Not acceptable");

  /**
   * "1007 indicates that an endpoint is terminating the connection because it has
   * received data within a message that was not consistent with the type of the message
   * (e.g., non-UTF-8 [RFC3629] data within a text message)."
   */
  CloseStatus BAD_DATA = new CloseStatus(1007, "Bad data");

  /**
   * "1008 indicates that an endpoint is terminating the connection because it has
   * received a message that violates its policy. This is a generic status code that can
   * be returned when there is no other more suitable status code (e.g., 1003 or 1009)
   * or if there is a need to hide specific details about the policy."
   */
  CloseStatus POLICY_VIOLATION = new CloseStatus(1008, "Policy violation");

  /**
   * "1009 indicates that an endpoint is terminating the connection because it has
   * received a message that is too big for it to process."
   */
  CloseStatus TOO_BIG_TO_PROCESS = new CloseStatus(1009, "Too big to process");

  /**
   * "1010 indicates that an endpoint (client) is terminating the connection because it
   * has expected the server to negotiate one or more extension, but the server didn't
   * return them in the response message of the WebSocket handshake. The list of
   * extensions that are needed SHOULD appear in the /reason/ part of the Close frame.
   * Note that this status code is not used by the server, because it can fail the
   * WebSocket handshake instead."
   */
  CloseStatus REQUIRED_EXTENSION = new CloseStatus(1010, "Required extension");

  /**
   * "1011 indicates that a server is terminating the connection because it encountered
   * an unexpected condition that prevented it from fulfilling the request."
   */
  CloseStatus SERVER_ERROR = new CloseStatus(1011, "Server error");

  /**
   * "1012 indicates that the service is restarted. A client may reconnect, and if it
   * chooses to do, should reconnect using a randomized delay of 5 - 30s."
   */
  CloseStatus SERVICE_RESTARTED = new CloseStatus(1012, "Service restarted");

  /**
   * "1013 indicates that the service is experiencing overload. A client should only
   * connect to a different IP (when there are multiple for the target) or reconnect to
   * the same IP upon user action."
   */
  CloseStatus SERVICE_OVERLOAD = new CloseStatus(1013, "Service overload");

  /**
   * @return Current request path.
   */
  String path();

  /**
   * @return The currently matched pattern.
   */
  String pattern();

  /**
   * @return The currently matched path variables (if any).
   */
  Map<Object, String> vars();

  /**
   * @return The type this route can consumes, defaults is: {@code * / *}.
   */
  MediaType consumes();

  /**
   * @return The type this route can produces, defaults is: {@code * / *}.
   */
  MediaType produces();

  /**
   * Register a callback to execute when a new message arrive.
   *
   * @param callback A callback
   * @throws Exception If something goes wrong.
   */
  void onMessage(Callback<Mutant> callback) throws Exception;

  /**
   * Register an error callback to execute when an error is found.
   *
   * @param callback A callback
   */
  void onError(ErrCallback callback);

  /**
   * Register an close callback to execute when client close the web socket.
   *
   * @param callback A callback
   * @throws Exception If something goes wrong.
   */
  void onClose(Callback<CloseStatus> callback) throws Exception;

  /**
   * Gracefully closes the connection, after sending a description message
   *
   * @param code Close status code.
   * @param reason Close reason.
   */
  default void close(final int code, final String reason) {
    close(CloseStatus.of(code, reason));
  }

  /**
   * Gracefully closes the connection, after sending a description message
   *
   * @param code Close status code.
   */
  default void close(final int code) {
    close(CloseStatus.of(code));
  }

  /**
   * Gracefully closes the connection, after sending a description message
   */
  @Override
  default void close() {
    close(NORMAL);
  }

  /**
   * True if the websocket is still open.
   *
   * @return True if the websocket is still open.
   */
  boolean isOpen();

  /**
   * Gracefully closes the connection, after sending a description message
   *
   * @param status Close status code.
   */
  void close(CloseStatus status);

  /**
   * Resume the client stream.
   */
  void resume();

  /**
   * Pause the client stream.
   */
  void pause();

  /**
   * Immediately shuts down the connection.
   *
   * @throws Exception If something goes wrong.
   */
  void terminate() throws Exception;

  /**
   * Send data through the connection.
   *
   * If the web socket is closed this method throw an {@link Err} with {@link #NORMAL} close status.
   *
   * @param data Data to send.
   * @throws Exception If something goes wrong.
   */
  default void send(final Object data) throws Exception {
    send(data, SUCCESS, ERR);
  }

  /**
   * Send data through the connection.
   *
   * If the web socket is closed this method throw an {@link Err} with {@link #NORMAL} close status.
   *
   * @param data Data to send.
   * @param success A success callback.
   * @throws Exception If something goes wrong.
   */
  default void send(final Object data, final SuccessCallback success) throws Exception {
    send(data, success, ERR);
  }

  /**
   * Send data through the connection.
   *
   * If the web socket is closed this method throw an {@link Err} with {@link #NORMAL} close status.
   *
   * @param data Data to send.
   * @param err An err callback.
   * @throws Exception If something goes wrong.
   */
  default void send(final Object data, final ErrCallback err) throws Exception {
    send(data, SUCCESS, err);
  }

  /**
   * Send data through the connection.
   *
   * If the web socket is closed this method throw an {@link Err} with {@link #NORMAL} close status.
   *
   * @param data Data to send.
   * @param success A success callback.
   * @param err An err callback.
   * @throws Exception If something goes wrong.
   */
  void send(Object data, SuccessCallback success, ErrCallback err) throws Exception;

}
