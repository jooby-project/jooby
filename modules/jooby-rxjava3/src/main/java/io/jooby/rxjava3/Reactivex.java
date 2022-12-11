/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rxjava3;

import static io.jooby.ReactiveSupport.newSubscriber;
import static org.reactivestreams.FlowAdapters.toSubscriber;

import java.lang.reflect.Type;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Reified;
import io.jooby.ResultHandler;
import io.jooby.Route;
import io.jooby.internal.rxjava3.RxObserver;
import io.jooby.internal.rxjava3.RxSubscriber;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Rx reactive filter.
 *
 * @author edgar
 */
public class Reactivex implements ResultHandler {

  private static final Route.Filter RX =
      new Route.Filter() {
        @NonNull @Override
        public Route.Handler apply(@NonNull Route.Handler next) {
          return ctx -> {
            Object result = next.apply(ctx);
            if (result instanceof Flowable flow) {
              flow.subscribe(toSubscriber(newSubscriber(ctx)));
            } else if (result instanceof Single single) {
              single.subscribe(new RxSubscriber(ctx));
            } else if (result instanceof Observable observable) {
              observable.subscribe(new RxObserver(newSubscriber(ctx)));
            } else if (result instanceof Maybe maybe) {
              maybe.subscribe(new RxSubscriber(ctx));
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
   * Adapt/map a {@link Flowable}, {@link Single}, {@link Observable} and {@link Maybe} results as
   * HTTP responses.
   *
   * <pre>{@code
   * import io.jooby.rxjava3.Reactivex.rx;
   *
   * use(rx());
   *
   * get("/", ctx -> Single.from("Hello"));
   *
   * }</pre>
   *
   * @return Rx filter.
   */
  public static Route.Filter rx() {
    return RX;
  }

  @Override
  public boolean matches(@NonNull Type type) {
    Class raw = Reified.get(type).getRawType();
    return Single.class.isAssignableFrom(raw)
        || Flowable.class.isAssignableFrom(raw)
        || Maybe.class.isAssignableFrom(raw)
        || Observable.class.isAssignableFrom(raw);
  }

  @NonNull @Override
  public Route.Filter create() {
    return RX;
  }

  @Override
  public boolean isReactive() {
    return true;
  }
}
