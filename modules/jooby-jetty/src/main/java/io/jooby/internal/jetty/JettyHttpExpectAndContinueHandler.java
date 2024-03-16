/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import io.jooby.StatusCode;

public class JettyHttpExpectAndContinueHandler extends Handler.Abstract.Wrapper {
  public JettyHttpExpectAndContinueHandler(Handler next) {
    super(next);
  }

  @Override
  public boolean handle(Request request, Response response, Callback callback) throws Exception {
    if (request.getHeaders().contains(HttpHeader.EXPECT, HttpHeaderValue.CONTINUE.asString())) {
      response.writeInterim(StatusCode.CONTINUE_CODE, HttpFields.EMPTY);
    }
    return super.handle(request, response, callback);
  }
}
