/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.metrics;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.jooby.internal.metrics.NoCacheHeader;

import javax.annotation.Nonnull;
import java.util.SortedMap;
import java.util.TreeMap;

public class HealthCheckHandler implements Route.Handler {

  @Nonnull
  @Override
  public Object apply(@Nonnull Context ctx) {
    HealthCheckRegistry registry = ctx.require(HealthCheckRegistry.class);

    SortedMap<String, Result> checks = ctx.query("name").toOptional().map(name -> {
      SortedMap<String, Result> map = new TreeMap<>();
      map.put(name, registry.runHealthCheck(name));
      return map;
    }).orElseGet(registry::runHealthChecks);

    ctx.setResponseCode(checks.isEmpty()
        ? StatusCode.NOT_IMPLEMENTED
        : checks.values().stream()
            .filter(it -> !it.isHealthy())
            .findFirst()
            .map(it -> StatusCode.SERVER_ERROR)
            .orElse(StatusCode.OK));

    NoCacheHeader.add(ctx);

    return checks;
  }
}
