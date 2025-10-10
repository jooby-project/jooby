/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx;

import java.io.IOException;
import java.io.OutputStream;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;

/**
 * A handler/filter that know how to handle {@link io.vertx.core.Vertx} types like:
 *
 * <ul>
 *   <li>{@link Future}
 *   <li>{@link Promise}
 *   <li>{@link AsyncFile}
 * </ul>
 *
 * @author edgar
 * @since 4.0.8
 */
public class VertxHandler implements Route.Filter {
  private static class AsyncFileHandler {
    private final Context ctx;
    private final OutputStream out;
    private boolean errored;
    private boolean closed;

    public AsyncFileHandler(Context ctx) {
      this.ctx = ctx;
      this.out = ctx.responseStream();
    }

    public Handler<Buffer> toHandler() {
      return this::handle;
    }

    public Handler<Void> toEndHandler() {
      return this::handleEnd;
    }

    public Handler<Throwable> toErrorHandler() {
      return this::handleError;
    }

    private void handleEnd(Void unused) {
      if (!closed) {
        closed = true;
        try {
          out.close();
        } catch (IOException ex) {
          handleError(ex);
        }
      }
    }

    private void handleError(Throwable ex) {
      if (!errored) {
        errored = true;
        ctx.sendError(ex);
      } else {
        ctx.getRouter().getLog().error("Async file write resulted in exception", ex);
      }
    }

    private void handle(Buffer buffer) {
      if (!closed) {
        try {
          out.write(buffer.getBytes());
        } catch (IOException ex) {
          handleError(ex);
        }
      }
    }
  }

  @Override
  public @NonNull Route.Handler apply(Route.Handler next) {
    return ctx -> {
      var result = next.apply(ctx);
      if (ctx.isResponseStarted()) {
        // Return context to mark as handled
        return ctx;
      } else if (result instanceof Promise<?> promise) {
        return futureResult(ctx, promise.future());
      } else if (result instanceof Future<?> future) {
        return futureResult(ctx, future);
      } else if (result instanceof Buffer buffer) {
        return bufferResult(ctx, buffer);
      }
      return result;
    };
  }

  public static Route.Filter vertx() {
    return new VertxHandler();
  }

  private Context bufferResult(Context ctx, Buffer buffer) {
    return ctx.render(buffer);
  }

  private static Context futureResult(Context ctx, Future<?> future) {
    future.onComplete(
        ar -> {
          if (ar.succeeded()) {
            var value = ar.result();
            if (value instanceof Buffer buffer) {
              ctx.send(buffer.getBytes());
            } else if (value instanceof AsyncFile file) {
              var handler = new AsyncFileHandler(ctx);
              file.handler(handler.toHandler());
              file.endHandler(handler.toEndHandler());
              file.exceptionHandler(handler.toErrorHandler());
            } else {
              ctx.render(value);
            }
          } else {
            ctx.sendError(ar.cause());
          }
        });
    return ctx;
  }
}
