/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mutiny;

import static io.jooby.ReactiveSupport.newSubscriber;

import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.annotation.ResultType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Mutiny reactive filter.
 *
 * @author edgar
 */
@ResultType(
    types = {Uni.class, Mutiny.class},
    handler = "mutiny",
    nonBlocking = true)
public class Mutiny {

  private static final Route.Filter MUTINY =
      new Route.Filter() {

        private void after(Context ctx, Object value, Throwable failure) {
          Route.After after = ctx.getRoute().getAfter();
          if (after != null) {
            try {
              after.apply(ctx, value, failure);
            } catch (Exception unexpected) {
              Logger log = ctx.getRouter().getLog();
              log.debug("After invocation resulted in exception", unexpected);
            }
          }
        }

        @NonNull @Override
        public Route.Handler apply(@NonNull Route.Handler next) {
          return ctx -> {
            Object result = next.apply(ctx);
            if (ctx.isResponseStarted()) {
              // Return context to mark as handled
              return ctx;
            } else if (result instanceof Uni uni) {
              uni.subscribe()
                  .with(
                      value -> {
                        // fire after:
                        after(ctx, value, null);
                        // See https://github.com/jooby-project/jooby/issues/3486
                        if (!ctx.isResponseStarted() && value != ctx) {
                          // render:
                          ctx.render(value);
                        }
                      },
                      failure -> {
                        // fire after:
                        after(ctx, null, (Throwable) failure);
                        // send error:
                        ctx.sendError((Throwable) failure);
                      });
              // Return context to mark as handled
              return ctx;
            } else if (result instanceof Multi multi) {
              multi.subscribe(newSubscriber(ctx));
              // Return context to mark as handled
              return ctx;
            }
            return result;
          };
        }

        @Override
        public void setRoute(Route route) {
          route.setNonBlocking(true);
        }
      };

  /**
   * Adapt/map a {@link Uni} and {@link Mutiny} results as HTTP responses.
   *
   * <pre>{@code
   * import io.jooby.mutiny.Mutiny.mutiny;
   *
   * use(mutiny());
   *
   * get("/", ctx -> Uni.createFrom("Hello"));
   *
   * }</pre>
   *
   * @return Mutiny filter.
   */
  public static Route.Filter mutiny() {
    return MUTINY;
  }

  public static Route.Handler mutiny(Route.Handler next) {
    return MUTINY.then(next);
  }
}
