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

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.List;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;

public class RouteChain implements Route.Chain {

  private Iterator<Route> routes;

  private String prefix;

  public RouteChain(final List<Route> routes) {
    this(requireNonNull(routes, "Routes are required.").iterator());
  }

  public RouteChain(final Iterator<Route> routes) {
    this.routes = requireNonNull(routes, "Routes are required.");
  }

  @Override
  public void next(final String prefix, final Request req, final Response rsp) throws Exception {
    if (rsp.committed()) {
      return;
    }

    if (prefix != null) {
      this.prefix = prefix;
    }

    RouteImpl route = get(next(this.prefix));
    // set route
    set(req, route);
    set(rsp, route);

    route.handle(req, rsp, this);
  }

  private Route next(final String prefix) {
    Route route = routes.next();
    if (prefix == null) {
      return route;
    }
    while (!route.apply(prefix)) {
      route = routes.next();
    }
    return route;
  }

  private RouteImpl get(final Route next) {
    return (RouteImpl) Route.Forwarding.unwrap(next);
  }

  private void set(final Request req, final Route route) {
    RequestImpl root = (RequestImpl) Request.Forwarding.unwrap(req);
    root.route(route);
  }

  private void set(final Response rsp, final Route route) {
    ResponseImpl root = (ResponseImpl) Response.Forwarding.unwrap(rsp);
    root.route(route);
  }

}
