/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mutiny;

import static io.jooby.ReactiveSupport.newSubscriber;

import java.lang.reflect.Type;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Reified;
import io.jooby.ResultHandler;
import io.jooby.Route;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Mutiny reactive filter.
 *
 * @author edgar
 */
public class Mutiny implements ResultHandler {

  private static final Route.Filter MUTINY =
      new Route.Filter() {
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

  @Override
  public boolean matches(@NonNull Type type) {
    Class<?> raw = Reified.get(type).getRawType();
    return Uni.class.isAssignableFrom(raw) || Multi.class.isAssignableFrom(raw);
  }

  @NonNull @Override
  public Route.Filter create() {
    return MUTINY;
  }

  @Override
  public boolean isReactive() {
    return true;
  }
}
