package io.jooby.internal;

import io.jooby.Mode;
import io.jooby.Reified;
import io.jooby.Route.Handler;
import io.jooby.internal.handler.CompletionStageHandler;
import io.jooby.internal.handler.DefaultHandler;
import io.jooby.internal.handler.DetachHandler;
import io.jooby.internal.handler.ExecutorHandler;
import io.jooby.internal.handler.FlowPublisherHandler;
import io.jooby.internal.handler.WorkerHandler;
import io.jooby.internal.handler.PublisherHandler;
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
    if (Flow.Publisher.class.isAssignableFrom(type)) {
      return Pipeline::flowPublisher;
    }
    Optional<Class> single = loadClass(loader, "io.reactivex.Single");
    if (single.isPresent()) {
      if (single.get().isAssignableFrom(type)) {
        return Pipeline::single;
      }
    }
    Optional<Class> publisher = loadClass(loader, "org.reactivestreams.Publisher");
    if (publisher.isPresent()) {
      if (publisher.get().isAssignableFrom(type)) {
        return Pipeline::publisher;
      }
    }
    return (mode, route) -> next(mode, route.executor(), new DefaultHandler(route.pipeline()));
  }

  private static Handler completableFuture(Mode mode, RouteImpl next) {
    return next(mode, next.executor(),
        new DetachHandler(new CompletionStageHandler(next.pipeline())));
  }

  private static Handler publisher(Mode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new PublisherHandler(next.pipeline())));
  }

  private static Handler flowPublisher(Mode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new FlowPublisherHandler(next.pipeline())));
  }

  private static Handler single(Mode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new SingleHandler(next.pipeline())));
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
