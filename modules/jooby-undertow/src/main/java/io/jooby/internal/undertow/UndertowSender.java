/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import java.io.IOException;
import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Sender;
import io.jooby.buffer.DataBuffer;
import io.undertow.io.IoCallback;
import io.undertow.server.HttpServerExchange;

public class UndertowSender implements Sender {
  private final UndertowContext ctx;
  private final HttpServerExchange exchange;

  public UndertowSender(UndertowContext ctx, HttpServerExchange exchange) {
    this.ctx = ctx;
    this.exchange = exchange;
  }

  @Override
  public Sender write(@NonNull byte[] data, @NonNull Callback callback) {
    exchange.getResponseSender().send(ByteBuffer.wrap(data), newIoCallback(ctx, callback));
    return this;
  }

  @NonNull @Override
  public Sender write(@NonNull DataBuffer data, @NonNull Callback callback) {
    // TODO: Fix usage of databuffer
    exchange
        .getResponseSender()
        .send(data.readableByteBuffers().next(), newIoCallback(ctx, callback));
    return this;
  }

  @Override
  public void close() {
    ctx.destroy(null);
  }

  private static IoCallback newIoCallback(UndertowContext ctx, Callback callback) {
    return new IoCallback() {
      @Override
      public void onComplete(HttpServerExchange exchange, io.undertow.io.Sender sender) {
        callback.onComplete(ctx, null);
      }

      @Override
      public void onException(
          HttpServerExchange exchange, io.undertow.io.Sender sender, IOException exception) {
        try {
          callback.onComplete(ctx, exception);
        } finally {
          ctx.destroy(exception);
        }
      }
    };
  }
}
