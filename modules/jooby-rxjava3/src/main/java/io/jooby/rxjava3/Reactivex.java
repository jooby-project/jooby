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

public class Reactivex {

  public static Route.Filter rx() {
    return new Route.Filter() {
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
  }
}
