/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class MetricsDecorator implements Route.Decorator {

  @Nonnull
  @Override
  public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      MetricRegistry registry = ctx.require(MetricRegistry.class);
      Counter counter = registry.counter("request.actives");
      Timer.Context timer = registry.timer("request").time();

      counter.inc();

      ctx.onComplete(context -> {
        timer.stop();
        counter.dec();
        registry.meter("responses." + context.getResponseCode().value()).mark();
      });

      return next.apply(ctx);
    };
  }
}
