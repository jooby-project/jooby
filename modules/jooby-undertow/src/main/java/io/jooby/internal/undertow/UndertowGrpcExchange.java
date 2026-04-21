/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.xnio.IoUtils;
import org.xnio.channels.StreamSinkChannel;

import io.jooby.rpc.grpc.GrpcExchange;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpAttachments;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

public class UndertowGrpcExchange implements GrpcExchange {

  private final HttpServerExchange exchange;
  private boolean headersSent = false;
  private StreamSinkChannel responseChannel;

  public UndertowGrpcExchange(HttpServerExchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public String getRequestPath() {
    return exchange.getRequestPath();
  }

  @Override
  public String getHeader(String name) {
    return exchange.getRequestHeaders().getFirst(name);
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> map = new HashMap<>();
    for (HeaderValues values : exchange.getRequestHeaders()) {
      map.put(values.getHeaderName().toString(), values.getFirst());
    }
    return map;
  }

  @Override
  public void send(ByteBuffer payload, Consumer<Throwable> callback) {
    if (!headersSent) {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/grpc");
      this.responseChannel = exchange.getResponseChannel();
      headersSent = true;
    }

    // Write and immediately flush to prevent bidirectional deadlocks
    doWriteAndFlush(payload, callback);
  }

  private void doWriteAndFlush(ByteBuffer payload, Consumer<Throwable> callback) {
    try {
      int res = responseChannel.write(payload);

      if (payload.hasRemaining()) {
        // Wait for socket to become writable
        responseChannel
            .getWriteSetter()
            .set(
                ch -> {
                  try {
                    ch.write(payload);
                    if (!payload.hasRemaining()) {
                      ch.suspendWrites();
                      doFlush(callback); // Proceed to flush
                    }
                  } catch (IOException e) {
                    ch.suspendWrites();
                    callback.accept(e);
                  }
                });
        responseChannel.resumeWrites();
      } else {
        // Written fully, proceed to flush immediately
        doFlush(callback);
      }
    } catch (IOException e) {
      callback.accept(e);
    }
  }

  private void doFlush(Consumer<Throwable> callback) {
    try {
      if (responseChannel.flush()) {
        callback.accept(null); // Fully flushed to network
      } else {
        // Wait for socket to become flushable
        responseChannel
            .getWriteSetter()
            .set(
                ch -> {
                  try {
                    if (ch.flush()) {
                      ch.suspendWrites();
                      callback.accept(null);
                    }
                  } catch (IOException e) {
                    ch.suspendWrites();
                    callback.accept(e);
                  }
                });
        responseChannel.resumeWrites();
      }
    } catch (IOException e) {
      callback.accept(e);
    }
  }

  @Override
  public void close(int statusCode, String description) {
    if (headersSent) {
      exchange.putAttachment(
          HttpAttachments.RESPONSE_TRAILER_SUPPLIER,
          () -> {
            HeaderMap trailers = new HeaderMap();
            trailers.put(HttpString.tryFromString("grpc-status"), String.valueOf(statusCode));
            if (description != null) {
              trailers.put(HttpString.tryFromString("grpc-message"), description);
            }
            return trailers;
          });

      try {
        responseChannel.shutdownWrites();
        if (!responseChannel.flush()) {
          responseChannel
              .getWriteSetter()
              .set(
                  ch -> {
                    try {
                      if (ch.flush()) {
                        ch.suspendWrites();
                        endExchange();
                      }
                    } catch (IOException ignored) {
                      ch.suspendWrites();
                      endExchange();
                    }
                  });
          responseChannel.resumeWrites();
        } else {
          endExchange();
        }
      } catch (IOException e) {
        endExchange();
      }

    } else {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/grpc");
      exchange
          .getResponseHeaders()
          .put(HttpString.tryFromString("grpc-status"), String.valueOf(statusCode));
      if (description != null) {
        exchange.getResponseHeaders().put(HttpString.tryFromString("grpc-message"), description);
      }
      exchange.endExchange();
    }
  }

  private void endExchange() {
    IoUtils.safeClose(responseChannel);
    IoUtils.safeClose(exchange.getRequestChannel());
  }
}
