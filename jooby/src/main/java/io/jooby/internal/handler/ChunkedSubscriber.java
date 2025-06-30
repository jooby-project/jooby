/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import java.util.concurrent.Flow;

import org.slf4j.Logger;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.Sender;
import io.jooby.Server;
import io.jooby.buffer.BufferedOutput;

public class ChunkedSubscriber implements Flow.Subscriber {

  private static final byte JSON_LBRACKET = '[';
  private static final byte JSON_SEP = ',';
  private static final byte[] JSON_RBRACKET = {']'};
  private Flow.Subscription subscription;
  private final Context ctx;
  private Sender sender;
  private MediaType responseType;

  public ChunkedSubscriber(Context ctx) {
    this.ctx = ctx;
  }

  @Override
  public void onSubscribe(Flow.Subscription subscription) {
    this.subscription = subscription;
    this.subscription.request(1);
  }

  public void onNext(Object item) {
    try {
      var route = ctx.getRoute();
      var after = route.getAfter();
      if (after != null) {
        after.apply(ctx, item, null);
      }
      var encoder = route.getEncoder();
      var data = encoder.encode(ctx, item);

      if (responseType == null) {
        responseType = ctx.getResponseType();
        if (responseType.isJson()) {
          data = prepend(ctx, data, JSON_LBRACKET);
        }
      } else {
        if (responseType.isJson()) {
          data = prepend(ctx, data, JSON_SEP);
        }
      }

      sender()
          .write(
              data,
              (context, x) -> {
                if (x == null) {
                  subscription.request(1);
                } else {
                  onError(x, true);
                }
              });
    } catch (Exception x) {
      onError(x, true);
    }
  }

  public void onError(Throwable x) {
    onError(x, false);
  }

  private void onError(Throwable x, boolean cancel) {
    // we use it to mark the response as errored so we don't sent a possible trailing json response.
    responseType = null;
    try {
      Route.After after = ctx.getRoute().getAfter();
      if (after != null) {
        try {
          after.apply(ctx, null, x);
        } catch (Exception unexpected) {
          x.addSuppressed(unexpected);
        }
      }
      Logger log = ctx.getRouter().getLog();
      if (Server.connectionLost(x)) {
        log.debug("connection lost: {} {}", ctx.getMethod(), ctx.getRequestPath(), x);
      } else {
        ctx.sendError(x);
      }
    } finally {
      if (cancel) {
        subscription.cancel();
      }
    }
  }

  public void onComplete() {
    if (responseType != null && responseType.isJson()) {
      responseType = null;
      sender()
          .write(
              JSON_RBRACKET,
              (ctx, x) -> {
                if (x != null) {
                  onError(x);
                }
              });
    }
    sender().close();
  }

  private static BufferedOutput prepend(Context ctx, BufferedOutput data, byte c) {
    var buffer = ctx.getOutputFactory().newCompositeOutput();
    buffer.write(c);
    data.transferTo(buffer::write);
    return buffer;
  }

  private Sender sender() {
    if (this.sender == null) {
      this.sender = ctx.responseSender();
    }
    return sender;
  }
}
