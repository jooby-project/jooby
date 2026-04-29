/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx;

import java.io.IOException;
import java.io.OutputStream;

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
public class VertxHandler implements Route.Reactive {
  private static class AsyncFileHandler {
    private final Context ctx;
    private final OutputStream out;
    private final AsyncFile file;
    private boolean errored;
    private boolean closed;

    public AsyncFileHandler(Context ctx, AsyncFile file) {
      this.ctx = ctx;
      this.out = ctx.responseStream();
      this.file = file;
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
        } finally {
          file.close();
        }
      }
    }

    private void handleError(Throwable ex) {
      if (!errored) {
        errored = true;
        try {
          file.close();
        } catch (Exception ignored) {
          // Ignore close errors if we are already handling an exception
        }
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

  /** Default constructor. */
  public VertxHandler() {}

  @Override
  public Route.Handler apply(Route.Handler next) {
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

  /**
   * Handle vertx types like: {@link Future}, {@link Promise} {@link AsyncFile} and {@link Buffer}.
   *
   * @return Vertx filter.
   */
  public static Route.Filter vertx() {
    return new VertxHandler();
  }

  private Context bufferResult(Context ctx, Buffer buffer) {
    return ctx.send(buffer.getBytes());
  }

  private static Context futureResult(Context ctx, Future<?> future) {
    future.onComplete(
        ar -> {
          if (ar.succeeded()) {
            var value = ar.result();
            if (value instanceof Buffer buffer) {
              ctx.send(buffer.getBytes());
            } else if (value instanceof AsyncFile file) {
              var handler = new AsyncFileHandler(ctx, file);
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
