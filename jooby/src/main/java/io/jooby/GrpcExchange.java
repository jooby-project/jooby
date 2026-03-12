/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.Nullable;

/** Server-agnostic abstraction for HTTP/2 trailing-header exchanges. */
public interface GrpcExchange {

  String getRequestPath();

  @Nullable String getHeader(String name);

  Map<String, String> getHeaders();

  /** Write framed bytes to the underlying non-blocking socket. */
  void send(ByteBuffer payload, Consumer<Throwable> onFailure);

  /**
   * Closes the HTTP/2 stream with trailing headers.
   *
   * @param statusCode The gRPC status code (e.g., 0 for OK, 12 for UNIMPLEMENTED).
   * @param description Optional status message.
   */
  void close(int statusCode, String description);
}
