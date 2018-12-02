package io.jooby.internal;

import io.jooby.ExecutionMode;
import io.jooby.Reified;
import io.jooby.Route.Handler;
import io.jooby.internal.handler.CompletionStageHandler;
import io.jooby.internal.handler.DefaultHandler;
import io.jooby.internal.handler.DetachHandler;
import io.jooby.internal.handler.WorkerExecHandler;
import io.jooby.internal.handler.FlowPublisherHandler;
import io.jooby.internal.handler.FluxHandler;
import io.jooby.internal.handler.MaybeHandler;
import io.jooby.internal.handler.MonoHandler;
import io.jooby.internal.handler.ObservableHandler;
import io.jooby.internal.handler.WorkerHandler;
import io.jooby.internal.handler.PublisherHandler;
import io.jooby.internal.handler.SingleHandler;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;

public class Pipeline {

  public static Handler compute(ClassLoader loader, RouteImpl route, ExecutionMode mode) {
    return provider(loader, Reified.rawType(route.returnType())).apply(mode, route);
  }

  private static BiFunction<ExecutionMode, RouteImpl, Handler> provider(ClassLoader loader, Class type) {
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
    Optional<Class> flowable = loadClass(loader, "io.reactivex.Flowable");
    if (flowable.isPresent()) {
      if (flowable.get().isAssignableFrom(type)) {
        return Pipeline::publisher;
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
    /** Flow API + ReactiveStream: */
    Optional<Class> publisher = loadClass(loader, "org.reactivestreams.Publisher");
    if (publisher.isPresent()) {
      if (publisher.get().isAssignableFrom(type)) {
        return Pipeline::publisher;
      }
    }
    if (Flow.Publisher.class.isAssignableFrom(type)) {
      return Pipeline::flowPublisher;
    }
    return (mode, route) -> next(mode, route.executor(), new DefaultHandler(route.pipeline()),
        true);
  }

  private static Handler completableFuture(ExecutionMode mode, RouteImpl next) {
    return next(mode, next.executor(),
        new DetachHandler(new CompletionStageHandler(next.pipeline())), false);
  }

  private static Handler publisher(ExecutionMode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new PublisherHandler(next.pipeline())),
        false);
  }

  private static Handler observable(ExecutionMode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new ObservableHandler(next.pipeline())),
        false);
  }

  private static Handler flux(ExecutionMode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new FluxHandler(next.pipeline())), false);
  }

  private static Handler mono(ExecutionMode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new MonoHandler(next.pipeline())), false);
  }

  private static Handler flowPublisher(ExecutionMode mode, RouteImpl next) {
    return next(mode, next.executor(),
        new DetachHandler(new FlowPublisherHandler(next.pipeline())), false);
  }

  private static Handler single(ExecutionMode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new SingleHandler(next.pipeline())),
        false);
  }

  private static Handler maybe(ExecutionMode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new MaybeHandler(next.pipeline())), false);
  }

  private static Handler next(ExecutionMode mode, Executor executor, Handler handler, boolean blocking) {
    if (executor == null) {
      if (mode == ExecutionMode.WORKER) {
        return new WorkerHandler(handler);
      }
      if (mode == ExecutionMode.DEFAULT && blocking) {
        return new WorkerHandler(handler);
      }
      return handler;
    }
    return new WorkerExecHandler(handler, executor);
  }

  private static Optional<Class> loadClass(ClassLoader loader, String name) {
    try {
      return Optional.of(loader.loadClass(name));
    } catch (ClassNotFoundException x) {
      return Optional.empty();
    }
  }
}
