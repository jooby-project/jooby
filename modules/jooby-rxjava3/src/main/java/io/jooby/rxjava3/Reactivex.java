/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rxjava3;

import static io.jooby.ReactiveSupport.newSubscriber;
import static org.reactivestreams.FlowAdapters.toSubscriber;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;
import io.jooby.internal.rxjava3.RxObserver;
import io.jooby.internal.rxjava3.RxSubscriber;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Rx reactive filter.
 *
 * @author edgar
 */
public class Reactivex {

  private static final Route.Filter RX =
      new Route.Reactive() {
        @Override
        public Route.Handler apply(@NonNull Route.Handler next) {
          return ctx -> {
            Object result = next.apply(ctx);
            if (ctx.isResponseStarted()) {
              // Return context to mark as handled
              return ctx;
            } else if (result instanceof Flowable flow) {
              flow.subscribe(toSubscriber(newSubscriber(ctx)));
              // Return context to mark as handled
              return ctx;
            } else if (result instanceof Single single) {
              single.subscribe(new RxSubscriber(ctx));
              // Return context to mark as handled
              return ctx;
            } else if (result instanceof Observable observable) {
              observable.subscribe(new RxObserver(newSubscriber(ctx)));
              // Return context to mark as handled
              return ctx;
            } else if (result instanceof Maybe maybe) {
              maybe.subscribe(new RxSubscriber(ctx));
              // Return context to mark as handled
              return ctx;
            } else if (result instanceof Disposable) {
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

  public static Route.Handler rx(Route.Handler next) {
    return RX.then(next);
  }
}
