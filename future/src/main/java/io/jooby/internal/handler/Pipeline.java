package io.jooby.internal.handler;

import io.jooby.Mode;
import io.jooby.Route;
import io.jooby.Route.Handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Pipeline {

  private static final Map<Class, BiFunction<Mode, Route, Handler>> providers = new LinkedHashMap<>();

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

  public static Handler compute(Class returnType, Mode mode, Route route) {
    return provider(returnType).apply(mode, route);
  }

  private static BiFunction<Mode, Route, Handler> provider(Class type) {
    for (Map.Entry<Class, BiFunction<Mode, Route, Handler>> entry : providers.entrySet()) {
      if (entry.getKey().isAssignableFrom(type)) {
        return entry.getValue();
      }
    }
    return (mode, route) -> next(mode, new DefaultHandler(route.pipeline()));
  }

  private static Handler completableFuture(Mode mode, Route next) {
    return next(mode, new DetachHandler(new CompletableFutureHandler(next.pipeline())));
  }

  private static Handler publisher(Mode mode, Route next) {
    return next(mode, new DetachHandler(new PublisherHandler(next.pipeline())));
  }

  private static Handler single(Mode mode, Route next) {
    return next(mode, new DetachHandler(new SingleHandler(next.pipeline())));
  }

  private static Handler next(Mode mode, Handler handler) {
    if (mode == Mode.IO) {
      return new IOHandler(handler);
    }
    return handler;
  }

}
