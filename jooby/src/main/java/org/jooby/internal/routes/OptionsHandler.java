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

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Definition;
import org.jooby.Status;
import org.jooby.Verb;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

public class OptionsHandler implements Route.Handler {

  private Set<Definition> routeDefs;

  @Inject
  public OptionsHandler(final Set<Route.Definition> routeDefs) {
    this.routeDefs = requireNonNull(routeDefs, "Route definitions are required.");
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Exception {
    if (!rsp.header("Allow").toOptional(String.class).isPresent()) {
      Set<String> allow = new LinkedHashSet<>();
      Set<Verb> verbs = EnumSet.allOf(Verb.class);
      String path = req.path();
      verbs.remove(req.route().verb());
      for (Verb alt : verbs) {
        for (Route.Definition routeDef : routeDefs) {
          Optional<Route> route = routeDef.matches(alt, path, MediaType.all, MediaType.ALL);
          if (route.isPresent()) {
            allow.add(route.get().verb().name());
          }
        }
      }
      rsp.header("Allow", Joiner.on(", ").join(allow));
      rsp.length(0);
      rsp.status(Status.OK);
    }
  }

}
