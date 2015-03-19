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
package org.jooby.internal.routes;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.Set;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Definition;
import org.jooby.internal.RouteImpl;

import com.google.inject.Inject;

public class HeadHandler implements Route.Filter {

  private Set<Definition> routeDefs;

  @Inject
  public HeadHandler(final Set<Route.Definition> routeDefs) {
    this.routeDefs = requireNonNull(routeDefs, "Route definitions are required.");
  }

  @Override
  public void handle(final Request req, final Response rsp, final Route.Chain chain)
      throws Exception {

    String path = req.path();
    for (Route.Definition routeDef : routeDefs) {
      Optional<Route> route = routeDef
          .matches("GET", path, MediaType.all, MediaType.ALL);
      if (route.isPresent() && !route.get().pattern().contains("*")) {
        // route found
        rsp.length(0);
        ((RouteImpl) route.get()).handle(req, rsp, chain);
        return;
      }
    }
    // not handled, just call next
    chain.next(req, rsp);
  }

}
