package io.jooby.internal;

import io.jooby.Mode;
import io.jooby.Reified;
import io.jooby.Route.Handler;
import io.jooby.internal.handler.CompletionStageHandler;
import io.jooby.internal.handler.DefaultHandler;
import io.jooby.internal.handler.DetachHandler;
import io.jooby.internal.handler.ExecutorHandler;
import io.jooby.internal.handler.FlowPublisherHandler;
import io.jooby.internal.handler.FluxHandler;
import io.jooby.internal.handler.MaybeHandler;
import io.jooby.internal.handler.MonoHandler;
import io.jooby.internal.handler.ObservableHandler;
import io.jooby.internal.handler.WorkerHandler;
import io.jooby.internal.handler.FlowableHandler;
import io.jooby.internal.handler.SingleHandler;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;

public class Pipeline {

  public static Handler compute(ClassLoader loader, RouteImpl route, Mode mode) {
    return provider(loader, Reified.rawType(route.returnType())).apply(mode, route);
  }

  private static BiFunction<Mode, RouteImpl, Handler> provider(ClassLoader loader, Class type) {
    if (CompletionStage.class.isAssignableFrom(type)) {
      return Pipeline::completableFuture;
    }
    /** Rx 2: */
    // Single:
    Optional<Class> single = loadClass(loader, "io.reactivex.Single");
    if (single.isPresent()) {
      if (single.get().isAssignableFrom(type)) {
        return Pipeline::single;
      }
    }
    // Maybe:
    Optional<Class> maybe = loadClass(loader, "io.reactivex.Maybe");
    if (maybe.isPresent()) {
      if (maybe.get().isAssignableFrom(type)) {
        return Pipeline::maybe;
      }
    }
    // Flowable:
    Optional<Class> publisher = loadClass(loader, "io.reactivex.Flowable");
    if (publisher.isPresent()) {
      if (publisher.get().isAssignableFrom(type)) {
        return Pipeline::flowable;
      }
    }
    // Observable:
    Optional<Class> observable = loadClass(loader, "io.reactivex.Observable");
    if (observable.isPresent()) {
      if (observable.get().isAssignableFrom(type)) {
        return Pipeline::observable;
      }
    }
    /** Reactor: */
    // Flux:
    Optional<Class> flux = loadClass(loader, "reactor.core.publisher.Flux");
    if (flux.isPresent()) {
      if (flux.get().isAssignableFrom(type)) {
        return Pipeline::flux;
      }
    }
    // Mono:
    Optional<Class> mono = loadClass(loader, "reactor.core.publisher.Mono");
    if (mono.isPresent()) {
      if (mono.get().isAssignableFrom(type)) {
        return Pipeline::mono;
      }
    }
    /** Flow API: */
    if (Flow.Publisher.class.isAssignableFrom(type)) {
      return Pipeline::flowPublisher;
    }
    return (mode, route) -> next(mode, route.executor(), new DefaultHandler(route.pipeline()));
  }

  private static Handler completableFuture(Mode mode, RouteImpl next) {
    return next(mode, next.executor(),
        new DetachHandler(new CompletionStageHandler(next.pipeline())));
  }

  private static Handler flowable(Mode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new FlowableHandler(next.pipeline())));
  }

  private static Handler observable(Mode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new ObservableHandler(next.pipeline())));
  }

  private static Handler flux(Mode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new FluxHandler(next.pipeline())));
  }

  private static Handler mono(Mode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new MonoHandler(next.pipeline())));
  }

  private static Handler flowPublisher(Mode mode, RouteImpl next) {
    return next(mode, next.executor(),
        new DetachHandler(new FlowPublisherHandler(next.pipeline())));
  }

  private static Handler single(Mode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new SingleHandler(next.pipeline())));
  }

  private static Handler maybe(Mode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new MaybeHandler(next.pipeline())));
  }

  private static Handler next(Mode mode, Executor executor, Handler handler) {
    if (executor == null) {
      return mode == Mode.WORKER ? new WorkerHandler(handler) : handler;
    }
    return new ExecutorHandler(handler, executor);
  }

  private static Optional<Class> loadClass(ClassLoader loader, String name) {
    try {
      return Optional.of(loader.loadClass(name));
    } catch (ClassNotFoundException x) {
      return Optional.empty();
    }
  }
}
