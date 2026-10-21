/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.sse;

import io.jooby.Context;
import io.jooby.ServerSentEmitter;
import io.jooby.annotation.Path;
import io.jooby.annotation.PathParam;
import io.jooby.annotation.SSE;

@Path("/events")
public class SseController {
  @SSE({"/sse", "/alias"})
  public void sse(ServerSentEmitter emitter) {
    emitter.send("Welcome");
    emitter.onClose(() -> {});
    emitter.close();
  }

  @SSE(path = "/{name}")
  public void named(Context ctx, @PathParam String name, ServerSentEmitter emitter)
      throws Exception {
    emitter.send(name + ":" + ctx.getRequestPath());
  }
}
