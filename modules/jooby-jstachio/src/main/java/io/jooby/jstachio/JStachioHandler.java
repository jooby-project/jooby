/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.io.IOException;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.Route.Handler;
import io.jstach.jstachio.JStachio;
import io.jstach.jstachio.Template;

class JStachioHandler extends JStachioRenderer<Context> implements Route.Filter {

  public JStachioHandler(JStachio jstachio, JStachioBuffer buffer) {
    super(jstachio, buffer);
  }

  @Override
  public Handler apply(Handler next) {
    return ctx -> {
      try {
        Object model = next.apply(ctx);
        return render(ctx, model);
      } catch (Throwable x) {
        ctx.sendError(x);
        return x;
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Override
  Context render(
      Context ctx,
      @SuppressWarnings("rawtypes") Template template,
      Object model,
      ByteBufferedOutputStream stream)
      throws IOException {
    ctx.setResponseType(MediaType.html);
    template.write(model, stream);
    /*
     * Rocker used a byte buffer here BUT it just wraps the internal buffer in the stream
     * instead of copying.
     *
     * Which is good for performance but bad if the ctx.send call is not blocking aka
     * hand the buffer off to another thread.
     */
    ctx.send(stream.toBuffer());
    return ctx;
  }
}
