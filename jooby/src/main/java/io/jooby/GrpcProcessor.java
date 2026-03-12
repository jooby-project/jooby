/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

import edu.umd.cs.findbugs.annotations.NonNull;

/** Intercepts and processes gRPC exchanges. */
public interface GrpcProcessor {

  /** Checks if the given URI path exactly matches a registered gRPC method. */
  boolean isGrpcMethod(String path);

  /**
   * @return A subscriber that the server will feed ByteBuffer chunks into, or null if the exchange
   *     was rejected/unimplemented.
   */
  Flow.Subscriber<ByteBuffer> process(@NonNull GrpcExchange exchange);
}
