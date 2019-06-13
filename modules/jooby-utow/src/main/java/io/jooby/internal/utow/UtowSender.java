/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import io.jooby.Sender;
import io.undertow.io.IoCallback;
import io.undertow.server.HttpServerExchange;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;

public class UtowSender implements Sender {
  private final UtowContext ctx;
  private final HttpServerExchange exchange;

  public UtowSender(UtowContext ctx, HttpServerExchange exchange) {
    this.ctx = ctx;
    this.exchange = exchange;
  }

  @Override public Sender write(@Nonnull byte[] data, @Nonnull Callback callback) {
    exchange.getResponseSender().send(ByteBuffer.wrap(data), newIoCallback(ctx, callback));
    return this;
  }

  @Override public void close() {
    ctx.destroy(null);
  }

  private static IoCallback newIoCallback(UtowContext ctx, Callback callback) {
    return new IoCallback() {
      @Override public void onComplete(HttpServerExchange exchange, io.undertow.io.Sender sender) {
        callback.onComplete(ctx, null);
      }

      @Override public void onException(HttpServerExchange exchange, io.undertow.io.Sender sender,
          IOException exception) {
        try {
          callback.onComplete(ctx, exception);
        } finally {
          ctx.destroy(exception);
        }
      }
    };
  }
}
