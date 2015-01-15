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
package org.jooby.internal;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.Route;
import org.jooby.WebSocket;

public class AppPrinter {

  private Set<Route.Definition> routes;

  private Set<WebSocket.Definition> sockets;

  private String url;

  @Inject
  public AppPrinter(final Set<Route.Definition> routes,
      final Set<WebSocket.Definition> sockets,
      @Named("application.host") final String host,
      @Named("application.port") final int port,
      @Named("application.path") final String path) {
    this.routes = routes;
    this.sockets = sockets;
    this.url = "http://" + host + ":" + port + path;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();

    routes(buffer);

    buffer.append("\nlistening on:\n  ").append(url);

    return buffer.toString();
  }

  private void routes(final StringBuilder buffer) {
    int verbMax = 0, routeMax = 0, consumesMax = 0, producesMax = 0;
    for (Route.Definition route : routes) {
      verbMax = Math.max(verbMax, route.verb().length());

      routeMax = Math.max(routeMax, route.pattern().length());

      consumesMax = Math.max(consumesMax, route.consumes().toString().length());

      producesMax = Math.max(producesMax, route.produces().toString().length());
    }

    String format = "  %-" + verbMax + "s %-" + routeMax + "s    %" + consumesMax + "s     %"
        + producesMax + "s    (%s)\n";

    for (Route.Definition route : routes) {
      buffer.append(String.format(format, route.verb(), route.pattern(),
          route.consumes(), route.produces(), route.name()));
    }

    sockets(buffer, verbMax, routeMax, consumesMax, producesMax);
  }

  private void sockets(final StringBuilder buffer, final int verbMax, int routeMax,
      int consumesMax,
      int producesMax) {
    for (WebSocket.Definition socket : sockets) {
      routeMax = Math.max(routeMax, socket.pattern().length());

      consumesMax = Math.max(consumesMax, socket.consumes().toString().length() + 2);

      producesMax = Math.max(producesMax, socket.produces().toString().length() + 2);
    }

    String format = "  %-" + verbMax + "s %-" + routeMax + "s    %" + consumesMax + "s     %"
        + producesMax + "s\n";

    for (WebSocket.Definition socketDef : sockets) {
      buffer.append(String.format(format, "WS", socketDef.pattern(),
          "[" + socketDef.consumes() + "]", "[" + socketDef.produces() + "]"));
    }
  }
}
