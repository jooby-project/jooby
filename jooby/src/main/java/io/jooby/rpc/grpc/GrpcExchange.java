/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rpc.grpc;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Server-agnostic abstraction for a native HTTP/2 gRPC exchange.
 *
 * <p>This interface bridges the gap between the underlying web server (Undertow, Netty, or Jetty)
 * and the reactive gRPC processor. Because gRPC heavily relies on HTTP/2 multiplexing, asynchronous
 * I/O, and trailing headers (trailers) to communicate status codes, standard HTTP/1.1 context
 * abstractions are insufficient.
 *
 * <p>Native server interceptors wrap their respective request/response objects into this interface,
 * allowing the {@link GrpcProcessor} to read headers, push zero-copy byte frames, and finalize the
 * stream with standard gRPC trailers without knowing which server engine is actually running.
 */
public interface GrpcExchange {

  /**
   * Retrieves the requested URI path.
   *
   * <p>In gRPC, this dictates the routing and strictly follows the pattern: {@code
   * /Fully.Qualified.ServiceName/MethodName}.
   *
   * @return The exact path of the incoming HTTP/2 request.
   */
  String getRequestPath();

  /**
   * Retrieves the value of the specified HTTP request header.
   *
   * @param name The name of the header (case-insensitive).
   * @return The header value, or {@code null} if the header is not present.
   */
  @Nullable String getHeader(String name);

  /**
   * Retrieves all HTTP request headers.
   *
   * @return A map containing all headers provided by the client.
   */
  Map<String, String> getHeaders();

  /**
   * Writes a gRPC-framed byte payload to the underlying non-blocking socket.
   *
   * <p>This method must push the buffer to the native network layer without blocking the invoking
   * thread (which is typically a background gRPC worker). The implementation is responsible for
   * translating the ByteBuffer into the server's native data format and flushing it over the
   * network.
   *
   * @param payload The properly framed 5-byte-prefixed gRPC payload to send.
   * @param onFailure A callback invoked immediately if the asynchronous network write fails (e.g.,
   *     if the client abruptly disconnects or the channel is closed).
   */
  void send(ByteBuffer payload, Consumer<Throwable> onFailure);

  /**
   * Closes the HTTP/2 stream by appending the mandatory gRPC trailing headers.
   *
   * <p>In the gRPC specification, a successful response or an application-level error is
   * communicated not by standard HTTP status codes (which are always 200 OK), but by appending
   * HTTP/2 trailers ({@code grpc-status} and {@code grpc-message}) at the very end of the stream.
   *
   * <p>Calling this method informs the native server to write those trailing headers and formally
   * close the bidirectional stream.
   *
   * @param statusCode The gRPC integer status code (e.g., {@code 0} for OK, {@code 12} for
   *     UNIMPLEMENTED, {@code 4} for DEADLINE_EXCEEDED).
   * @param description An optional, human-readable status message detailing the result or error.
   *     May be {@code null}.
   */
  void close(int statusCode, @Nullable String description);
}
