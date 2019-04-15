/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import io.jooby.Router;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JettyHandler extends AbstractHandler {
  private final Router router;
  private final boolean defaultHeaders;
  private final int bufferSize;
  private final long maxRequestSize;

  public JettyHandler(Router router, int bufferSize, long maxRequestSize, boolean defaultHeaders) {
    this.router = router;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
    this.defaultHeaders = defaultHeaders;
  }

  @Override public void handle(String target, Request request, HttpServletRequest servletRequest,
      HttpServletResponse response) {
    response.setContentType("text/plain");
    if (defaultHeaders) {
      response.setHeader(HttpHeader.SERVER.asString(), "jetty");
    }
    JettyContext context = new JettyContext(request, router, bufferSize, maxRequestSize);
    router.match(context).execute(context);
  }
}
