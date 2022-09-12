/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;

import io.jooby.Router;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

public class JettyServlet extends GenericServlet {
  private Router router;
  private boolean defaultHeaders;
  private int bufferSize;
  private long maxRequestSize;

  public JettyServlet(Router router, int bufferSize, long maxRequestSize, boolean defaultHeaders) {
    this.router = router;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
    this.defaultHeaders = defaultHeaders;
  }

  @Override public void service(ServletRequest req, ServletResponse rsp) {
    Request request = (Request) req;
    HttpServletResponse response = (HttpServletResponse) rsp;
    response.setContentType("text/plain");
    if (defaultHeaders) {
      response.setHeader(HttpHeader.SERVER.asString(), "J");
    }
    JettyContext context = new JettyContext(request, router, bufferSize, maxRequestSize);
    router.match(context).execute(context);
  }
}
