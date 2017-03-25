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

import java.util.List;
import java.util.Map;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;

import com.google.common.collect.ImmutableMap;

public class RouteChain implements Route.Chain {

  private List<Route> routes;

  private String prefix;

  private int i = 0;

  private RequestImpl rreq;

  private ResponseImpl rrsp;

  private boolean hasAttrs;

  public RouteChain(final RequestImpl req, final ResponseImpl rsp, final List<Route> routes) {
    this.routes = routes;
    this.rreq = req;
    this.rrsp = rsp;

    // eager decision if we need to wrap a route to get all the attrs within the change.
    this.hasAttrs = hasAttributes(routes);
  }

  private boolean hasAttributes(final List<Route> routes) {
    for (int i = 0; i < routes.size(); i++) {
      if (routes.get(i).attributes().size() > 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void next(final String prefix, final Request req, final Response rsp) throws Throwable {
    if (rsp.committed()) {
      return;
    }

    if (prefix != null) {
      this.prefix = prefix;
    }

    Route route = next(this.prefix);
    // set route
    rreq.route(hasAttrs ? attrs(route, routes, i - 1) : route);
    rrsp.route(route);

    get(route).handle(req, rsp, this);
  }

  private Route next(final String prefix) {
    Route route = routes.get(i++);
    if (prefix == null) {
      return route;
    }
    while (!route.apply(prefix)) {
      route = routes.get(i++);
    }
    return route;
  }

  private RouteWithFilter get(final Route next) {
    return (RouteWithFilter) Route.Forwarding.unwrap(next);
  }

  private static Route attrs(final Route route, final List<Route> routes, final int i) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    for (int t = i; t < routes.size(); t++) {
      builder.putAll(routes.get(t).attributes());
    }
    Map<String, Object> attrs = builder.build();
    return new Route.Forwarding(route) {
      @Override
      public Map<String, Object> attributes() {
        return attrs;
      }
    };
  }

}
