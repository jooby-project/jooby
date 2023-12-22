/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.io.IOException;
import java.util.function.BiFunction;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Route.Handler;
import io.jstach.jstachio.JStachio;
import io.jstach.jstachio.output.ByteBufferEncodedOutput;

class JStachioHandler extends JStachioRenderer<Context> implements Route.Filter {

  public JStachioHandler(
      JStachio jstachio,
      JStachioBuffer buffer,
      BiFunction<Context, String, String> contextFunction) {
    super(jstachio, buffer, contextFunction);
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

  @Override
  Context extractOutput(Context ctx, ByteBufferEncodedOutput stream) throws IOException {
    ctx.send(stream.asByteBuffer());
    return ctx;
  }
}
