/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler.reactive;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.MessageEncoder;
import io.jooby.Route;
import io.jooby.Sender;
import io.jooby.Server;
import org.slf4j.Logger;

public class ChunkedSubscriber {

  private static final byte JSON_LBRACKET = '[';
  private static final byte JSON_SEP = ',';
  private static final byte[] JSON_RBRACKET = {']'};
  private ChunkedSubscription subscription;
  private final Context ctx;
  private final Sender sender;
  private MediaType responseType;

  public ChunkedSubscriber(Context ctx) {
    this.ctx = ctx;
    this.sender = ctx.responseSender();
  }

  public void onSubscribe(ChunkedSubscription subscription) {
    this.subscription = subscription;
    this.subscription.request(1);
  }

  public void onNext(Object item) {
    try {
      Route route = ctx.getRoute();
      MessageEncoder encoder = route.getEncoder();
      byte[] data = encoder.encode(ctx, item);

      if (responseType == null) {
        responseType = ctx.getResponseType();
        if (responseType.isJson()) {
          data = prepend(data, JSON_LBRACKET);
        }
      } else {
        if (responseType.isJson()) {
          data = prepend(data, JSON_SEP);
        }
      }

      sender.write(data, (context, x) -> {
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
      sender.write(JSON_RBRACKET, (ctx, x) -> {
        if (x != null) {
          onError(x);
        }
      });
    }
    sender.close();
  }

  private static byte[] prepend(byte[] data, byte c) {
    byte[] tmp = new byte[data.length + 1];
    System.arraycopy(data, 0, tmp, 1, data.length);
    tmp[0] = c;
    return tmp;
  }
}
