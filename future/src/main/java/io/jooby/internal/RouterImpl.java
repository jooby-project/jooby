package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Err;
import io.jooby.ErrorHandler;
import io.jooby.Filter;
import io.jooby.Handler;
import io.jooby.Renderer;
import io.jooby.RootHandler;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.StatusCode;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RouterImpl implements Router {

  private static class Stack {
    private String pattern;

    private List<Filter> filters = new ArrayList<>();

    public Stack(String pattern) {
      this.pattern = pattern;
    }

    public void then(Filter filter) {
      filters.add(filter);
    }

    public Stream<Filter> toFilter() {
      return filters.stream().filter(Objects::nonNull);
    }
  }

  private ErrorHandler err;

  private Map<String, StatusCode> errorCodes;

  private final Chi chi = new Chi();

  private LinkedList<Stack> stack = new LinkedList<>();

  private List<Route> routes = new ArrayList<>();

  public RouterImpl() {
    stack.addLast(new Stack(""));
  }

  @Nonnull @Override public Router renderer(@Nonnull Renderer renderer) {
    stack.peekLast().then(renderer);
    return this;
  }

  @Override @Nonnull
  public Router filter(@Nonnull Filter filter) {
    stack.peekLast().then(filter);
    return this;
  }

  @Nonnull @Override public Router error(@Nonnull ErrorHandler handler) {
    err = err == null ? handler : err.then(handler);
    return this;
  }

  @Override @Nonnull
  public Router dispatch(@Nonnull Runnable action) {
    filter(next -> ctx -> ctx.dispatch(asRunnable(ctx, next)));
    return newGroup("", action);
  }

  @Override @Nonnull
  public Router dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    filter(next -> ctx -> ctx.dispatch(executor, asRunnable(ctx, next)));
    return newGroup("", action);
  }

  @Override @Nonnull
  public Router group(@Nonnull Runnable action) {
    return newGroup("", action);
  }

  @Override @Nonnull
  public Router path(@Nonnull String pattern, @Nonnull Runnable action) {
    return newGroup(pattern, action);
  }

  @Override
  public Route route(@Nonnull String method, @Nonnull String pattern, @Nonnull Handler handler) {
    /** Pattern: */
    StringBuilder pat = new StringBuilder();
    stack.forEach(it -> pat.append(it.pattern));
    pat.append(pattern);
    /** Filters: */
    List<Filter> filters = stack.stream()
        .flatMap(Stack::toFilter)
        .collect(Collectors.toList());
    /** Renderer: */
    Filter renderer = findLast(filters, Renderer.class::isInstance, Renderer.TO_STRING);
    /** Handler: */
    Handler h =  renderer.then(handler);
    if (filters.size() > 0) {
      h = filters.stream().skip(1)
          .reduce(filters.get(0), Filter::then)
          .then(h);
    }
    /** Route: */
    Route route = new Route(method, pat.toString(), h);
    chi.insertRoute(route.method(), route.pattern(), h);
    routes.add(route);
    return route;
  }

  @Nonnull @Override public RootHandler asRootHandler(@Nonnull Handler handler) {
    return new RootHandlerImpl(handler, err, log(), this::errorCode);
  }

  @Nonnull public Router start() {
    if (err == null) {
      err = ErrorHandler.DEFAULT;
    }
    this.stack.removeLast().filters.clear();
    this.stack = null;
    return this;
  }

  @Override public Handler match(String method, String path) {
    return chi.findRoute(method, path).handler;
  }

  @Nonnull @Override
  public Router errorCode(@Nonnull Class<? extends Throwable> type, @Nonnull StatusCode statusCode) {
    if (errorCodes == null) {
      errorCodes = new HashMap<>();
    }
    this.errorCodes.put(type.getCanonicalName(), statusCode);
    return this;
  }

  @Nonnull @Override public StatusCode errorCode(@Nonnull Throwable x) {
    if (x instanceof Err) {
      return ((Err) x).statusCode;
    }
    if (errorCodes != null) {
      Class type = x.getClass();
      while (type != Throwable.class) {
        StatusCode errorCode = errorCodes.get(type.getCanonicalName());
        if (errorCode != null) {
          return errorCode;
        }
        type = type.getSuperclass();
      }
    }
    if (x instanceof IllegalArgumentException) {
      return StatusCode.BAD_REQUEST;
    }
    if (x instanceof NoSuchElementException) {
      return StatusCode.BAD_REQUEST;
    }
    if (x instanceof FileNotFoundException) {
      return StatusCode.NOT_FOUND;
    }
    return StatusCode.SERVER_ERROR;
  }

  @Override public String toString() {
    StringBuilder buff = new StringBuilder();
    int size = IntStream.range(0, routes.size())
        .map(i -> routes.get(i).method().length() + 1)
        .max()
        .orElse(0);

    routes.forEach(
        r -> buff.append(String.format("\n  %-" + size + "s", r.method())).append(r.pattern()));
    return buff.substring(1);
  }

  private Router newGroup(@Nonnull String pattern, @Nonnull Runnable action) {
    stack.addLast(new Stack(pattern));
    action.run();
    stack.removeLast().filters.clear();
    return this;
  }

  private Runnable asRunnable(Context ctx, Handler next) {
    return () -> asRootHandler(next).apply(ctx);
  }

  private Filter findLast(List<Filter> filters, Predicate<Filter> predicate, Filter fallback) {
    for (int i = filters.size() - 1; i >= 0; i--) {
      Filter it = filters.get(i);
      if (predicate.test(it)) {
        return it;
      }
    }
    return fallback;
  }
}
