/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import io.jooby.GrpcExchange;
import io.jooby.GrpcProcessor;

public class JettyGrpcHandler extends Handler.Wrapper {

  private final GrpcProcessor processor;

  public JettyGrpcHandler(Handler next, GrpcProcessor processor) {
    this.processor = processor;
    setHandler(next);
  }

  @Override
  public boolean handle(Request request, Response response, Callback callback) throws Exception {
    String contentType = request.getHeaders().get("Content-Type");

    if (contentType != null && contentType.startsWith("application/grpc")) {

      if (!"HTTP/2.0".equals(request.getConnectionMetaData().getProtocol())) {
        response.setStatus(426); // Upgrade Required
        response.getHeaders().put("Connection", "Upgrade");
        response.getHeaders().put("Upgrade", "h2c");
        callback.succeeded();
        return true;
      }

      GrpcExchange exchange = new JettyGrpcExchange(request, response, callback);
      Flow.Subscriber<ByteBuffer> subscriber = processor.process(exchange);

      new JettyGrpcInputBridge(request, subscriber, callback).start();

      return true;
    }

    return super.handle(request, response, callback);
  }
}
