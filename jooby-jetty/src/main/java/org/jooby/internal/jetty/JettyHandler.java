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

import java.io.IOException;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.jooby.MediaType;
import org.jooby.servlet.ServletServletRequest;
import org.jooby.servlet.ServletServletResponse;
import org.jooby.servlet.ServletUpgrade;
import org.jooby.spi.HttpHandler;
import org.jooby.spi.NativeWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyHandler extends AbstractHandler {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private HttpHandler dispatcher;

  private WebSocketServerFactory webSocketServerFactory;

  private String tmpdir;

  private MultipartConfigElement multiPartConfig;

  public JettyHandler(final HttpHandler dispatcher,
      final WebSocketServerFactory webSocketServerFactory, final String tmpdir) {
    this.dispatcher = dispatcher;
    this.webSocketServerFactory = webSocketServerFactory;
    this.tmpdir = tmpdir;
    this.multiPartConfig = new MultipartConfigElement(tmpdir);
  }

  @Override
  public void handle(final String target, final Request baseRequest,
      final HttpServletRequest request, final HttpServletResponse response) throws IOException,
      ServletException {
    try {

      baseRequest.setHandled(true);

      String type = baseRequest.getContentType();
      boolean multipart = false;
      if (type != null && type.toLowerCase().startsWith(MediaType.multipart.name())) {
        baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multiPartConfig);
        multipart = true;
      }

      dispatcher.handle(
          new ServletServletRequest(request, tmpdir, multipart)
              .with(new ServletUpgrade() {

                @SuppressWarnings("unchecked")
                @Override
                public <T> T upgrade(final Class<T> type) throws Exception {
                  if (type == NativeWebSocket.class) {
                    if (webSocketServerFactory.isUpgradeRequest(request, response)) {
                      if (webSocketServerFactory.acceptWebSocket(request, response)) {
                        String key = JettyWebSocket.class.getName();
                        NativeWebSocket ws = (NativeWebSocket) request.getAttribute(key);
                        if (ws != null) {
                          request.removeAttribute(key);
                          return (T) ws;
                        }
                      }
                    }
                  }
                  throw new UnsupportedOperationException("Not Supported: " + type);
                }
              }),
          new ServletServletResponse(request, response)
          );
    } catch (IOException | ServletException | RuntimeException ex) {
      baseRequest.setHandled(false);
      log.error("execution of: " + target + " resulted in error", ex);
      throw ex;
    } catch (Throwable ex) {
      baseRequest.setHandled(false);
      log.error("execution of: " + target + " resulted in error", ex);
      throw new IllegalStateException(ex);
    }
  }

}
