/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mutiny;

import static io.jooby.ReactiveSupport.newSubscriber;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class Mutiny {

  public static Route.Filter mutiny() {
    return new Route.Filter() {
      @NonNull @Override
      public Route.Handler apply(@NonNull Route.Handler next) {
        return ctx -> {
          Object result = next.apply(ctx);
          if (result instanceof Uni uni) {
            uni.subscribe().with(ctx::render, x -> ctx.sendError((Throwable) x));
          } else if (result instanceof Multi multi) {
            multi.subscribe(newSubscriber(ctx));
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
