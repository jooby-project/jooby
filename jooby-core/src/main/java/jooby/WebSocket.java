package jooby;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import jooby.internal.RouteMatcher;
import jooby.internal.RoutePattern;
import jooby.internal.WebSocketImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public interface WebSocket extends Closeable {

  interface Handler {
    void connect(WebSocket ws) throws Exception;
  }

  class CloseStatus {
    private final int code;

    private final String reason;

    /**
     * Create a new {@link CloseStatus} instance.
     *
     * @param code the status code
     */
    public CloseStatus(final int code) {
      this(code, null);
    }

    /**
     * Create a new {@link CloseStatus} instance.
     *
     * @param code the status code
     * @param reason the reason
     */
    public CloseStatus(final int code, final String reason) {
      Preconditions.checkArgument((code >= 1000 && code < 5000), "Invalid code: %s", code);
      this.code = code;
      this.reason = reason;
    }

    /**
     * Returns the status code.
     */
    public int code() {
      return this.code;
    }

    /**
     * Returns the reason or {@code null}.
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

  interface Callback<T> extends Serializable {
    void invoke(T data) throws Exception;
  }

  interface Callback0 extends Serializable {
    void invoke() throws Exception;
  }

  class Definition {
    private String name = "anonymous";

    /**
     * A route pattern.
     */
    private RoutePattern routePattern;

    /**
     * Defines the media types that the methods of a resource class or can accept. Default is:
     * {@literal *}/{@literal *}.
     */
    private MediaType consumes = MediaType.all;

    /**
     * Defines the media types that the methods of a resource class or can produces. Default is:
     * {@literal *}/{@literal *}.
     */
    private MediaType produces = MediaType.all;

    private String pattern;

    private Handler handler;

    /**
     * Creates a new {@link Definition}.
     *
     * @param pattern
     * @param filter
     */
    public Definition(final String pattern, final Handler handler) {
      requireNonNull(pattern, "A route path is required.");
      requireNonNull(handler, "A handler is required.");

      this.routePattern = new RoutePattern("WS", pattern);
      // normalized pattern
      this.pattern = routePattern.pattern();
      this.handler = handler;
    }

    public String pattern() {
      return pattern;
    }

    public Optional<WebSocket> matches(final String path) {
      RouteMatcher matcher = routePattern.matcher("WS" + path);
      if (matcher.matches()) {
        return Optional.of(asWebSocket(matcher));
      }
      return Optional.empty();
    }

    public String name() {
      return name;
    }

    public Definition name(final String name) {
      this.name = requireNonNull(name, "A socket's name is required.");
      return this;
    }

    /**
     * @param candidate A media type to test.
     * @return True, if the route can consume the given media type.
     */
    public boolean canConsume(@Nonnull final MediaType candidate) {
      return MediaType.matcher(Arrays.asList(candidate)).matches(consumes);
    }

    /**
     * @param candidates A media types to test.
     * @return True, if the route can produces the given media type.
     */
    public boolean canProduce(final List<MediaType> candidates) {
      return MediaType.matcher(candidates).matches(produces);
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
     * @param produces The media types to test for.
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
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      buffer.append("WS ").append(pattern()).append("\n");
      buffer.append("  name: ").append(name()).append("\n");
      buffer.append("  consume: ").append(consumes()).append("\n");
      buffer.append("  produces: ").append(produces()).append("\n");
      return buffer.toString();
    }

    private WebSocket asWebSocket(final RouteMatcher matcher) {
      return new WebSocketImpl(handler, matcher.path(), pattern, name, matcher.vars(),
          consumes, produces);
    }
  }

  /** The logging system. */
  Logger log = LoggerFactory.getLogger(WebSocket.class);

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

  String path();

  String pattern();

  String name();

  Map<String, String> vars();

  MediaType consume();

  MediaType produces();

  <T> void onMessage(Callback<Variant> callback) throws Exception;

  <T> void onError(Callback<Exception> callback) throws Exception;

  <T> void onClose(Callback<CloseStatus> callback) throws Exception;

  default void close(final int code, final String reason) {
    close(new CloseStatus(code, reason));
  }

  @Override
  default void close() {
    close(NORMAL);
  }

  void close(CloseStatus status);

  void resume();

  void pause();

  void terminate() throws Exception;

  default void send(final Object data) throws Exception {
    send(data, () -> {
    }, (ex) -> {
      log.error("Error while sending data", ex);
    });
  }

  default void send(final Object data, final Callback0 success) throws Exception {
    send(data, success, (ex) -> {
      log.error("Error while sending data", ex);
    });
  }

  default void send(final Object data, final Callback<Exception> err) throws Exception {
    send(data, () -> {
    }, err);
  }

  void send(Object data, Callback0 success, Callback<Exception> err) throws Exception;

  default <T> T getInstance(@Nonnull final Class<T> type) {
    return getInstance(Key.get(type));
  }

  @Nonnull
  default <T> T getInstance(@Nonnull final TypeLiteral<T> type) {
    return getInstance(Key.get(type));
  }

  @Nonnull
  <T> T getInstance(@Nonnull Key<T> key);

}
