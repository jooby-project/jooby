/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.util.List;
import java.util.Map;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class PrefixHandler extends Handler.Abstract {
  private final List<Map.Entry<String, Handler>> mapping;
  private int defaultHandlerIndex;

  public PrefixHandler(List<Map.Entry<String, Handler>> mapping) {
    this.mapping = mapping;
    this.defaultHandlerIndex = 0;
    for (int i = 0; i < mapping.size(); i++) {
      if (mapping.get(i).getKey().equals("/")) {
        defaultHandlerIndex = i;
        break;
      }
    }
  }

  @Override
  public boolean handle(Request request, Response response, Callback callback) throws Exception {
    for (Map.Entry<String, Handler> e : mapping) {
      var path = request.getHttpURI().getPath();
      if (path.startsWith(e.getKey())) {
        return e.getValue().handle(request, response, callback);
      }
    }
    return mapping.get(defaultHandlerIndex).getValue().handle(request, response, callback);
  }
}
