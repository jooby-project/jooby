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

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.util.Set;
import java.util.function.Function;

import org.jooby.Route;
import org.jooby.WebSocket;
import org.slf4j.Logger;

import com.google.common.base.Strings;
import com.typesafe.config.Config;

public class AppPrinter {

  private Set<Route.Definition> routes;

  private Set<WebSocket.Definition> sockets;

  private String[] urls;

  private boolean http2;

  private boolean h2cleartext;

  public AppPrinter(final Set<Route.Definition> routes,
      final Set<WebSocket.Definition> sockets,
      final Config conf) {
    this.routes = routes;
    this.sockets = sockets;
    String host = conf.getString("application.host");
    String port = conf.getString("application.port");
    String path = conf.getString("application.path");
    this.urls = new String[2];
    this.urls[0] = "http://" + host + ":" + port + path;
    if (conf.hasPath("application.securePort")) {
      this.urls[1] = "https://" + host + ":" + conf.getString("application.securePort") + path;
    }
    http2 = conf.getBoolean("server.http2.enabled");
    h2cleartext = conf.getBoolean("server.http2.cleartext");
  }

  public void printConf(final Logger log, final Config conf) {
    if (log.isDebugEnabled()) {
      String desc = configTree(conf.origin().description());
      log.debug("config tree:\n{}", desc);
    }
  }

  private String configTree(final String description) {
    return configTree(description.split(":\\s+\\d+,|,"), 0);
  }

  private String configTree(final String[] sources, final int i) {
    if (i < sources.length) {
      return new StringBuilder()
          .append(Strings.padStart("", i, ' '))
          .append("└── ")
          .append(sources[i])
          .append("\n")
          .append(configTree(sources, i + 1))
          .toString();
    }
    return "";
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();

    routes(buffer);
    String[] h2 = {h2(" ", http2 && h2cleartext), h2("", http2) };
    buffer.append("\nlistening on:");
    for (int i = 0; i < urls.length; i++) {
      if (urls[i] != null) {
        buffer.append("\n  ").append(urls[i]).append(h2[i]);
      }
    }
    return buffer.toString();
  }

  private String h2(final String prefix, final boolean h2) {
    return h2 ? prefix + " +h2" : "";
  }

  private void routes(final StringBuilder buffer) {
    Function<Route.Definition, String> p = route -> {
      return Match(route.filter()).of(
          Case(instanceOf(Route.Before.class), "{before}" + route.pattern()),
          Case(instanceOf(Route.After.class), "{after}" + route.pattern()),
          Case(instanceOf(Route.Complete.class), "{complete}" + route.pattern()),
          Case($(), route.pattern()));
    };

    int verbMax = 0, routeMax = 0, consumesMax = 0, producesMax = 0;
    for (Route.Definition route : routes) {
      verbMax = Math.max(verbMax, route.method().length());

      routeMax = Math.max(routeMax, p.apply(route).length());

      consumesMax = Math.max(consumesMax, route.consumes().toString().length());

      producesMax = Math.max(producesMax, route.produces().toString().length());
    }

    String format = "  %-" + verbMax + "s %-" + routeMax + "s    %" + consumesMax
        + "s     %" + producesMax + "s    (%s)\n";

    for (Route.Definition route : routes) {
      buffer.append(
          String.format(format, route.method(), p.apply(route), route.consumes(),
              route.produces(), route.name()));
    }

    sockets(buffer, Math.max(verbMax, "WS".length()), routeMax, consumesMax, producesMax);
  }

  private void sockets(final StringBuilder buffer, final int verbMax, int routeMax,
      int consumesMax, int producesMax) {
    for (WebSocket.Definition socket : sockets) {
      routeMax = Math.max(routeMax, socket.pattern().length());

      consumesMax = Math.max(consumesMax, socket.consumes().toString().length() + 2);

      producesMax = Math.max(producesMax, socket.produces().toString().length() + 2);
    }

    String format = "  %-" + verbMax + "s %-" + routeMax + "s    %" + consumesMax + "s     %"
        + producesMax + "s\n";

    for (WebSocket.Definition socket : sockets) {
      buffer.append(String.format(format, "WS", socket.pattern(),
          "[" + socket.consumes() + "]", "[" + socket.produces() + "]"));
    }
  }
}
