/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.jetty;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.jooby.MediaType;
import org.jooby.internal.RouteHandler;

import com.typesafe.config.Config;

public class JettyHandler extends SessionHandler {

  private RouteHandler handler;

  private final MultipartConfigElement multiPartConfig;

  private WebSocketServerFactory webSocketFactory;

  public JettyHandler(final RouteHandler handler, final Config config) {
    this.handler = requireNonNull(handler, "A route handler is required.");
    multiPartConfig = new MultipartConfigElement(config.getString("java.io.tmpdir"));
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();
    webSocketFactory = getBean(WebSocketServerFactory.class);
  }

  @Override
  public void doHandle(final String requestURI, final Request baseRequest,
      final HttpServletRequest req, final HttpServletResponse res) throws IOException,
      ServletException {

    if (webSocketFactory != null && webSocketFactory.isUpgradeRequest(req, res)) {
      // We have an upgrade request
      if (webSocketFactory.acceptWebSocket(req, res)) {
        // We have a socket instance created
        baseRequest.setHandled(true);
        return;
      }
      // If we reach this point, it means we had an incoming request to upgrade
      // but it was either not a proper websocket upgrade, or it was possibly rejected
      // due to incoming request constraints (controlled by WebSocketCreator)
      if (res.isCommitted()) {
        // not much we can do at this point.
        return;
      }
    }
    String type = req.getContentType();
    if (type != null && type.startsWith(MediaType.multipart.name())) {
      baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multiPartConfig);
    }
    try {
      handler.handle(req, res);
      // mark as handled
      baseRequest.setHandled(true);
    } catch (RuntimeException | IOException ex) {
      baseRequest.setHandled(false);
      throw ex;
    } catch (Exception ex) {
      baseRequest.setHandled(false);
      throw new ServletException("Unexpected error", ex);
    }
  }
}
