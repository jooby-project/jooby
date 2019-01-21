package io.jooby.internal.handler.reactive;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Renderer;
import io.jooby.Route;
import io.jooby.Sender;
import io.jooby.Server;
import org.slf4j.Logger;

public class ChunkedSubscriber {

  private ChunkedSubscription subscription;
  private Context ctx;
  private Sender sender;
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
      Route route = ctx.route();
      Renderer renderer = route.renderer();
      byte[] data = renderer.encode(ctx, item);

      if (responseType == null) {
        responseType = ctx.type();
        if (responseType.isJson()) {
          data = prepend(data, (byte) '[');
        }
      } else {
        if (responseType.isJson()) {
          data = prepend(data, (byte) ',');
        }
      }

      sender.sendBytes(data, (context, x) -> {
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
    try {
      if (ctx.isResponseStarted()) {
        Logger log = ctx.router().log();
        if (x != null) {
          if (Server.connectionLost(x)) {
            log.debug("exception found while sending response {} {}", ctx.method(),
                ctx.pathString(),
                x);
          } else {
            log.error("exception found while sending response {} {}", ctx.method(),
                ctx.pathString(),
                x);
          }
        }
      } else {
        ctx.sendError(x);
      }
    } finally {
      try {
        if (cancel) {
          subscription.cancel();
        }
      } finally {
        onComplete();
      }
    }
  }

  public void onComplete() {
    if (responseType != null && responseType.isJson()) {
      responseType = null;
      sender.sendBytes(new byte[]{']'}, (ctx, x) -> {
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
