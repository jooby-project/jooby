/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Core Service Provider Interface (SPI) for the gRPC extension.
 *
 * <p>This processor acts as the bridge between the native HTTP/2 web servers (Undertow, Netty,
 * Jetty) and the embedded gRPC engine. It is designed to intercept and process gRPC exchanges at
 * the lowest possible network level, completely bypassing Jooby's standard HTTP/1.1 routing
 * pipeline. This architecture ensures strict HTTP/2 specification compliance, zero-copy buffering,
 * and reactive backpressure.
 */
public interface GrpcProcessor {

  /**
   * Checks if the given URI path exactly matches a registered gRPC method.
   *
   * <p>Native server interceptors (Undertow, Netty, Jetty) use this method as a lightweight,
   * fail-fast guard. If this returns {@code true}, the server will hijack the request and upgrade
   * it to a native gRPC stream. If {@code false}, the request safely falls through to the standard
   * Jooby router (typically resulting in a standard HTTP 404 Not Found, which gRPC clients
   * gracefully translate to Status 12 UNIMPLEMENTED).
   *
   * @param path The incoming request path (e.g., {@code /fully.qualified.Service/MethodName}).
   * @return {@code true} if the path is mapped to an active gRPC service; {@code false} otherwise.
   */
  boolean isGrpcMethod(String path);

  /**
   * Initiates the reactive gRPC pipeline for an incoming HTTP/2 request.
   *
   * <p>When a valid gRPC request is intercepted, the native server wraps the underlying network
   * connection into a {@link GrpcExchange} and passes it to this method. The processor uses this
   * exchange to asynchronously send response headers, payload byte frames, and HTTP/2 trailers.
   *
   * @param exchange The native server exchange representing the bidirectional HTTP/2 stream.
   * @return A reactive {@link Flow.Subscriber} that the native server must feed incoming request
   *     payload {@link ByteBuffer} chunks into. Returns {@code null} if the exchange was rejected.
   * @throws IllegalStateException If an unregistered path bypasses the {@link
   *     #isGrpcMethod(String)} guard.
   */
  Flow.Subscriber<ByteBuffer> process(@NonNull GrpcExchange exchange);
}
