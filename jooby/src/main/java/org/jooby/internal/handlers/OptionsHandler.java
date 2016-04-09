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
package org.jooby.internal.handlers;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashSet;
import java.util.Set;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Definition;
import org.jooby.Status;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

public class OptionsHandler implements Route.Handler {

  private static final String SEP = ", ";

  private static final String ALLOW = "Allow";

  private Set<Definition> routes;

  @Inject
  public OptionsHandler(final Set<Route.Definition> routes) {
    this.routes = requireNonNull(routes, "Routes are required.");
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Exception {
    if (!rsp.header(ALLOW).isSet()) {
      Set<String> allow = new LinkedHashSet<>();
      Set<String> methods = new LinkedHashSet<>(Route.METHODS);
      String path = req.path();
      methods.remove(req.method());
      for (String method : methods) {
        routes.stream()
            .filter(route -> route.matches(method, path, MediaType.all, MediaType.ALL).isPresent())
            .forEach(route -> allow.add(route.method()));
      }
      rsp.header(ALLOW, Joiner.on(SEP).join(allow))
          .length(0)
          .status(Status.OK);
    }
  }

}
