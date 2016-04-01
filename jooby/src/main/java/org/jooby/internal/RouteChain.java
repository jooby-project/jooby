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

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;

public class RouteChain implements Route.Chain {

  private List<Route> routes;

  private String prefix;

  private int i = 0;

  private RequestImpl rreq;

  private ResponseImpl rrsp;

  public RouteChain(final RequestImpl req, final ResponseImpl rsp, final List<Route> routes) {
    this.routes = routes;
    this.rreq = req;
    this.rrsp = rsp;
  }

  @Override
  public void next(final String prefix, final Request req, final Response rsp) throws Throwable {
    if (rsp.committed()) {
      return;
    }

    if (prefix != null) {
      this.prefix = prefix;
    }

    RouteImpl route = get(next(this.prefix));
    // set route
    rreq.route(route);
    rrsp.route(route);

    route.handle(req, rsp, this);
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

  private RouteImpl get(final Route next) {
    return (RouteImpl) Route.Forwarding.unwrap(next);
  }

}
