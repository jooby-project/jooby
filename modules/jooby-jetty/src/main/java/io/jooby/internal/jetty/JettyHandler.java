/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.util.function.Function;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import io.jooby.Router;
import io.jooby.ServiceKey;
import io.jooby.internal.jetty.http2.JettyHeaders;

public class JettyHandler extends Handler.Abstract {
  private final Router router;
  private final boolean defaultHeaders;
  private final int bufferSize;
  private final long maxRequestSize;

  public JettyHandler(
      InvocationType invocationType,
      Router router,
      int bufferSize,
      long maxRequestSize,
      boolean defaultHeaders) {
    super(invocationType);
    this.router = router;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
    this.defaultHeaders = defaultHeaders;
  }

  @Override
  public boolean handle(Request request, Response response, Callback callback) {
    var responseHeaders = response.getHeaders();
    responseHeaders.put(JettyHeaders.TEXT_PLAIN);
    if (defaultHeaders) {
      responseHeaders.put(JettyHeaders.SERVER);
    }
    try {
      var context =
          new JettyContext(
              getInvocationType(), request, response, callback, router, bufferSize, maxRequestSize);
      if (!"POST".equalsIgnoreCase(request.getMethod())
          || !request.getHeaders().contains(HttpHeader.CONTENT_TYPE, "application/grpc")) {
        router.match(context).execute(context);
      } else {
        var subscriber = router.require(ServiceKey.key(Function.class, "gRPC"));
        new JettyGrpcHandler(context, subscriber).handle(request, response, callback);
      }
    } catch (JettyStopPipeline ignored) {
      // handled already,
    }
    return true;
  }
}
