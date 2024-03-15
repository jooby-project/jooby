/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import io.jooby.Router;

public class JettyHandler extends Handler.Abstract.NonBlocking {
  private Router router;
  private boolean defaultHeaders;
  private int bufferSize;
  private long maxRequestSize;

  public JettyHandler(Router router, int bufferSize, long maxRequestSize, boolean defaultHeaders) {
    this.router = router;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
    this.defaultHeaders = defaultHeaders;
  }

  @Override
  public boolean handle(Request request, Response response, Callback callback) {
    var responseHeaders = response.getHeaders();
    responseHeaders.put(HttpHeader.CONTENT_TYPE, "text/plain");
    if (defaultHeaders) {
      responseHeaders.put(HttpHeader.SERVER.asString(), "J");
    }
    var context = new JettyContext(request, response, callback, router, bufferSize, maxRequestSize);
    router.match(context).execute(context);
    return true;
  }
}
