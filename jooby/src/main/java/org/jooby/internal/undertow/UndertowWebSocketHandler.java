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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.internal.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Methods;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;

import java.util.Optional;
import java.util.Set;

import org.jooby.WebSocket;
import org.jooby.WebSocket.Definition;
import org.jooby.internal.WebSocketImpl;

public class UndertowWebSocketHandler extends WebSocketProtocolHandshakeHandler {

  public static final AttachmentKey<WebSocketImpl> SOCKET =
      AttachmentKey.create(WebSocketImpl.class);

  private Set<Definition> sockets;

  private HttpHandler next;

  public UndertowWebSocketHandler(final WebSocketConnectionCallback callback,
      final HttpHandler next, final Set<WebSocket.Definition> sockets) {
    super(callback, next);
    this.next = next;
    this.sockets = sockets;
  }

  @Override
  public void handleRequest(final HttpServerExchange exchange) throws Exception {
    if (!exchange.getRequestMethod().equals(Methods.GET)) {
      // Only GET is supported to start the handshake
      next.handleRequest(exchange);
      return;
    }
    String path = exchange.getRequestPath();
    for (WebSocket.Definition socketDef : sockets) {
      Optional<WebSocket> matches = socketDef.matches(path);
      if (matches.isPresent()) {
        WebSocketImpl socket = (WebSocketImpl) matches.get();
        exchange.putAttachment(SOCKET, socket);
        super.handleRequest(exchange);
        return;
      }
    }
    // no luck, call next
    this.next.handleRequest(exchange);
  }

}
