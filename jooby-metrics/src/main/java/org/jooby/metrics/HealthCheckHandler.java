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
package org.jooby.metrics;

import java.util.SortedMap;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route.Handler;
import org.jooby.Status;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.collect.ImmutableSortedMap;

/**
 * Produces a:
 * <ul>
 * <li>501: if the registry is empty (no health checks)</li>
 * <li>200: if all the health checks are healthy</li>
 * <li>500: otherwise</li>
 * </ul>
 *
 * @author edgar
 * @since 0.13.0
 */
public class HealthCheckHandler implements Handler {

  @Override
  public void handle(final Request req, final Response rsp) throws Throwable {
    HealthCheckRegistry registry = req.require(HealthCheckRegistry.class);
    SortedMap<String, Result> checks = req.param("name").toOptional().map(name -> {
      SortedMap<String, Result> set = ImmutableSortedMap.of(name, registry.runHealthCheck(name));
      return set;
    }).orElseGet(() -> registry.runHealthChecks());

    final Status status;
    if (checks.isEmpty()) {
      status = Status.NOT_IMPLEMENTED;
    } else {
      status = checks.values().stream()
          .filter(it -> !it.isHealthy())
          .findFirst()
          .map(it -> Status.SERVER_ERROR)
          .orElse(Status.OK);
    }
    rsp.status(status)
        .header("Cache-Control", "must-revalidate,no-cache,no-store")
        .send(checks);
  }

}
