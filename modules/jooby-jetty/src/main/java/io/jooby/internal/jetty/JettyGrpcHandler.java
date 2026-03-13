/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import io.jooby.rpc.grpc.GrpcProcessor;

public class JettyGrpcHandler extends Handler.Wrapper {

  private final GrpcProcessor processor;

  public JettyGrpcHandler(Handler next, GrpcProcessor processor) {
    this.processor = processor;
    setHandler(next);
  }

  @Override
  public boolean handle(Request request, Response response, Callback callback) throws Exception {
    var contentType = request.getHeaders().get("Content-Type");

    if (processor.isGrpcMethod(request.getHttpURI().getPath())
        && contentType != null
        && contentType.startsWith("application/grpc")) {

      if (!"HTTP/2.0".equals(request.getConnectionMetaData().getProtocol())) {
        response.setStatus(426); // Upgrade Required
        response.getHeaders().put("Connection", "Upgrade");
        response.getHeaders().put("Upgrade", "h2c");
        callback.succeeded();
        return true;
      }

      var exchange = new JettyGrpcExchange(request, response, callback);
      var subscriber = processor.process(exchange);

      new JettyGrpcInputBridge(request, subscriber, callback).start();

      return true;
    }

    // not grpc, move next
    return super.handle(request, response, callback);
  }
}
