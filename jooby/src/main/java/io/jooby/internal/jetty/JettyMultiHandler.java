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

import io.jooby.App;
import io.jooby.Router;
import org.eclipse.jetty.server.Request;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class JettyMultiHandler extends JettyHandler {
  private final List<App> routers;

  public JettyMultiHandler(Router router, List<App> routers) {
    super(router);
    this.routers = routers;
  }

  @Override public void handle(String target, Request request, HttpServletRequest servletRequest,
      HttpServletResponse response) throws IOException, ServletException {
    for (Router router : routers) {
      JettyContext ctx = new JettyContext(request, router.worker(), router.errorHandler(), router.tmpdir());
      Router.Match match = router.match(ctx);
      if (match.matches()) {
        handleMatch(target, request, response, ctx, router, match.route());
        return;
      }
    }
  }
}
