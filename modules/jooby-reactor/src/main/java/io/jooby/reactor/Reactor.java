/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.reactor;

import static io.jooby.ReactiveSupport.newSubscriber;
import static org.reactivestreams.FlowAdapters.toSubscriber;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Reactor {

  public static Route.Filter reactor() {
    return new Route.Filter() {
      @NonNull @Override
      public Route.Handler apply(@NonNull Route.Handler next) {
        return ctx -> {
          Object result = next.apply(ctx);
          if (result instanceof Flux flux) {
            flux.subscribe(toSubscriber(newSubscriber(ctx)));
          } else if (result instanceof Mono mono) {
            mono.subscribe(ctx::render, x -> ctx.sendError((Throwable) x));
          }
          return result;
        };
      }

      @Override
      public void setRoute(Route route) {
        route.setReactive(true);
      }
    };
  }
}
