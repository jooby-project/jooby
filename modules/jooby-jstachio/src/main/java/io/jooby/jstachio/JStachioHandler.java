/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2023 Edgar Espina
 */
package io.jooby.jstachio;

import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.Route.Handler;

class JStachioHandler implements Route.Filter {
  
  private final JStachioMessageEncoder encoder;
  
  public JStachioHandler(JStachioMessageEncoder encoder) {
    super();
    this.encoder = encoder;
  }

  @Override
  public Handler apply(Handler next) {
    return ctx -> {
      try {
        Object model =  next.apply(ctx);
        ctx.setResponseType(MediaType.html);
        return ctx.send(encoder.render(model));
      } catch (Throwable x) {
        ctx.sendError(x);
        return x;
      }
    };
  }

}
