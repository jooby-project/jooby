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

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Chain;
import org.jooby.Status;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * Track request information like: active requests, request time and responses.
 *
 * @author edgar
 * @since 0.13.0
 */
public class InstrumentedHandler implements Route.Filter {

  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Exception {
    MetricRegistry registry = req.require(MetricRegistry.class);
    Counter counter = registry.counter("request.actives");
    Timer.Context timer = registry.timer("request").time();
    try {
      counter.inc();
      chain.next(req, rsp);
    } finally {
      timer.stop();
      counter.dec();
      Meter meter = registry.meter("responses." + rsp.status().orElse(Status.OK).value());
      meter.mark();
    }
  }

}
