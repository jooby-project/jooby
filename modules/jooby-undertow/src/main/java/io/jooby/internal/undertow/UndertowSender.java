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
import io.jooby.output.Output;
import io.undertow.io.IoCallback;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpAttachments;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;

public class UndertowSender implements Sender {
  private final UndertowContext ctx;
  private final HttpServerExchange exchange;
  private HeaderMap trailers;

  public UndertowSender(UndertowContext ctx) {
    this.ctx = ctx;
    this.exchange = ctx.exchange;
    this.trailers = ctx.trailers;
  }

  @Override
  public Sender setTrailer(@NonNull String name, @NonNull String value) {
    if (trailers == null) {
      trailers = new HeaderMap();
    }
    trailers.put(HttpString.tryFromString(name), value);
    return this;
  }

  @Override
  public Sender write(@NonNull byte[] data, @NonNull Callback callback) {
    return write(ByteBuffer.wrap(data), callback);
  }

  @NonNull @Override
  public Sender write(@NonNull Output output, @NonNull Callback callback) {
    return write(output.asByteBuffer(), callback);
  }

  private Sender write(@NonNull ByteBuffer buffer, @NonNull Callback callback) {
    if (trailers != null) {
      var copy = new HeaderMap();
      copy.putAll(trailers);
      trailers = null;
      exchange.putAttachment(HttpAttachments.RESPONSE_TRAILERS, copy);
    }
    exchange
        .getResponseSender()
        .send(
            buffer,
            new IoCallback() {
              @Override
              public void onComplete(HttpServerExchange exchange, io.undertow.io.Sender sender) {}

              @Override
              public void onException(
                  HttpServerExchange exchange,
                  io.undertow.io.Sender sender,
                  IOException exception) {}
            });
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
