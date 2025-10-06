/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx;

import java.io.IOException;
import java.io.OutputStream;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jooby.output.Output;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;

public class VertxEncoder implements MessageEncoder {
  private static class AsyncFileHandler {
    private Context ctx;
    private OutputStream out;
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

  @Nullable @Override
  public Output encode(@NonNull Context ctx, @NonNull Object value) {
    if (value instanceof Buffer buffer) {
      ctx.send(buffer.getBytes());
    } else if (value instanceof AsyncFile file) {
      var handler = new AsyncFileHandler(ctx);
      file.handler(handler.toHandler());
      file.endHandler(handler.toEndHandler());
      file.exceptionHandler(handler.toErrorHandler());
    }
    return null;
  }
}
