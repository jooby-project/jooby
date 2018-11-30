package io.jooby.internal.handler;

import io.jooby.Mode;
import io.jooby.Route.Handler;
import io.jooby.internal.RouteImpl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Pipeline {

  private static final Map<Class, BiFunction<Mode, RouteImpl, Handler>> providers = new LinkedHashMap<>();

  static {
    // TODO CLEAR STATE after startup
    providers.put(CompletableFuture.class, Pipeline::completableFuture);
    ClassLoader loader = Pipeline.class.getClassLoader();
    loadClass(loader, "io.reactivex.Single", type ->
        providers.put(type, Pipeline::single)
    );
    loadClass(Pipeline.class.getClassLoader(), "org.reactivestreams.Publisher", type ->
        providers.put(type, Pipeline::publisher)
    );
  }

  private static void loadClass(ClassLoader loader, String name, Consumer<Class> consumer) {
    try {
      consumer.accept(loader.loadClass(name));
    } catch (ClassNotFoundException x) {
      // ignore missing class
    }
  }

  public static Handler compute(Class returnType, Mode mode, RouteImpl route) {
    return provider(returnType).apply(mode, route);
  }

  private static BiFunction<Mode, RouteImpl, Handler> provider(Class type) {
    for (Map.Entry<Class, BiFunction<Mode, RouteImpl, Handler>> entry : providers.entrySet()) {
      if (entry.getKey().isAssignableFrom(type)) {
        return entry.getValue();
      }
    }
    return (mode, route) -> next(mode, route.executor(), new DefaultHandler(route.pipeline()));
  }

  private static Handler completableFuture(Mode mode, RouteImpl next) {
    return next(mode, next.executor(),
        new DetachHandler(new CompletableFutureHandler(next.pipeline())));
  }

  private static Handler publisher(Mode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new PublisherHandler(next.pipeline())));
  }

  private static Handler single(Mode mode, RouteImpl next) {
    return next(mode, next.executor(), new DetachHandler(new SingleHandler(next.pipeline())));
  }

  private static Handler next(Mode mode, Executor executor, Handler handler) {
    if (executor == null) {
      return mode == Mode.IO ? new IOHandler(handler) : handler;
    }
    return new ExecutorHandler(handler, executor);
  }

}
