/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.reactor;

import static io.jooby.ReactiveSupport.newSubscriber;
import static org.reactivestreams.FlowAdapters.toSubscriber;

import java.lang.reflect.Type;

import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Reified;
import io.jooby.ResultHandler;
import io.jooby.Route;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactor reactive filter.
 *
 * @author edgar
 */
public class Reactor implements ResultHandler {

  private static final Route.Filter REACTOR =
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
            } else if (result instanceof Flux flux) {
              flux.subscribe(toSubscriber(newSubscriber(ctx)));
              // Return context to mark as handled
              return ctx;
            } else if (result instanceof Mono mono) {
              mono.subscribe(
                  value -> {
                    // fire after:
                    after(ctx, value, null);
                    // render:
                    ctx.render(value);
                  },
                  failure -> {
                    // fire after:
                    after(ctx, null, (Throwable) failure);
                    // send error:
                    ctx.sendError((Throwable) failure);
                  });
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
   * Adapt/map a {@link Mono} and {@link Flux} results as HTTP responses.
   *
   * <pre>{@code
   * import io.jooby.reactor.Reactor.reactor;
   *
   * use(reactor());
   *
   * get("/", ctx -> Mono.create("Hello"));
   *
   * }</pre>
   *
   * @return Reactor filter.
   */
  public static Route.Filter reactor() {
    return REACTOR;
  }

  @Override
  public boolean matches(@NonNull Type type) {
    Class<?> raw = Reified.get(type).getRawType();
    return Mono.class.isAssignableFrom(raw) || Flux.class.isAssignableFrom(raw);
  }

  @NonNull @Override
  public Route.Filter create() {
    return REACTOR;
  }

  @Override
  public boolean isReactive() {
    return true;
  }
}
