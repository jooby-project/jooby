/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.ServerSentEmitter;
import io.jooby.StatusCode;

public class ServerSentEventHandler implements Route.Handler {
  private ServerSentEmitter.Handler handler;

  public ServerSentEventHandler(ServerSentEmitter.Handler handler) {
    this.handler = handler;
  }

  @NonNull @Override
  public Object apply(@NonNull Context ctx) {
    ctx.setResponseHeader("Connection", "Close");
    ctx.setResponseType("text/event-stream; charset=utf-8");
    ctx.setResponseCode(StatusCode.OK);

    ctx.upgrade(handler);
    return ctx;
  }
}
