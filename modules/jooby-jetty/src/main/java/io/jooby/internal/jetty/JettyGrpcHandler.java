/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.util.concurrent.Flow;
import java.util.function.Function;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import io.jooby.Context;

/** A professional Jetty Handler that bridges HTTP/2 streams to a gRPC Subscriber. */
public class JettyGrpcHandler extends Handler.Abstract {

  private final Function<Context, Flow.Subscriber<byte[]>> subscriberFactory;
  private final Context ctx;

  public JettyGrpcHandler(
      io.jooby.Context ctx, Function<Context, Flow.Subscriber<byte[]>> subscriberFactory) {
    this.ctx = ctx;
    this.subscriberFactory = subscriberFactory;
  }

  @Override
  public boolean handle(Request request, Response response, Callback callback) {
    Flow.Subscriber<byte[]> subscriber = subscriberFactory.apply(ctx);

    JettyRequestPublisher publisher = new JettyRequestPublisher(request);
    publisher.subscribe(subscriber);

    return true;
  }
}
