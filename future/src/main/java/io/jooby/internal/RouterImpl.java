package io.jooby.internal;

import io.jooby.BaseContext;
import io.jooby.Context;
import io.jooby.Err;
import io.jooby.Renderer;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.StatusCode;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RouterImpl implements Router {
  private static class Stack {
    private String pattern;

    private List<Route.Filter> filters = new ArrayList<>();
    private List<Renderer> renderers = new ArrayList<>();
    private List<Route.After> afters = new ArrayList<>();

    public Stack(String pattern) {
      this.pattern = pattern;
    }

    public void then(Renderer renderer) {
      renderers.add(renderer);
    }

    public void then(Route.Filter filter) {
      filters.add(filter);
    }

    public void then(Route.After after) {
      afters.add(after);
    }

    public Stream<Route.Filter> toFilter() {
      return filters.stream();
    }

    public Stream<Route.After> toAfter() {
      return afters.stream();
    }

    public Stream<Renderer> toRenderer() {
      return renderers.stream();
    }

    public void clear() {
      this.filters.clear();
      this.renderers.clear();
      this.afters.clear();
    }
  }

  private Route.ErrorHandler err;

  private Route.RootErrorHandler rootErr;

  private Map<String, StatusCode> errorCodes;

  private final $Chi chi = new $Chi();

  private LinkedList<Stack> stack = new LinkedList<>();

  private List<Route> routes = new ArrayList<>();

  private Renderer renderer = Renderer.TO_STRING;

  private String basePath;

  private Predicate<Context> predicate;

  private List<Router> routers;

  public RouterImpl() {
    stack.addLast(new Stack(""));
  }

  @Nonnull @Override public Router basePath(@Nonnull String basePath) {
    if (routes.size() > 0) {
      throw new IllegalStateException("Base path must be set before adding any routes.");
    }
    this.basePath = basePath;
    return this;
  }

  @Nonnull @Override public List<Route> routes() {
    return routes;
  }

  @Nonnull @Override public Router when(@Nonnull Predicate<Context> predicate) {
    this.predicate = predicate;
    return this;
  }

  @Nonnull @Override
  public Router use(@Nonnull Router router) {
    if (routers == null) {
      routers = new ArrayList<>();
    }
    routers.add(router);
    routes.addAll(router.routes());
    return this;
  }

  @Nonnull @Override public Router renderer(@Nonnull Renderer renderer) {
    this.renderer = renderer.then(this.renderer);
    stack.peekLast().then(renderer);
    return this;
  }

  @Nonnull @Override public Router gzip(@Nonnull Runnable action) {
    Route.Filter filter = next -> ctx -> {
      Route.Filter gzip = ctx.gzip();
      Route.Handler handler = gzip.apply(next);
      return handler.apply(ctx);
    };
    return newGroup("", action, filter);
  }

  @Override @Nonnull public Router filter(@Nonnull Route.Filter filter) {
    stack.peekLast().then(filter);
    return this;
  }

  @Override @Nonnull public Router after(@Nonnull Route.After after) {
    stack.peekLast().then(after);
    return this;
  }

  @Nonnull @Override public Router before(@Nonnull Route.Before before) {
    return filter(before);
  }

  @Nonnull @Override public Router error(@Nonnull Route.ErrorHandler handler) {
    err = err == null ? handler : err.then(handler);
    return this;
  }

  @Override @Nonnull public Router dispatch(@Nonnull Runnable action) {
    Route.Filter filter = next -> ctx -> ctx.dispatch(asRunnable(ctx, next));
    return newGroup("", action, filter);
  }

  @Override @Nonnull public Router dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    Route.Filter filter = next -> ctx -> ctx.dispatch(executor, asRunnable(ctx, next));
    return newGroup("", action, filter);
  }

  @Nonnull @Override public Route.Handler detach(@Nonnull Route.DetachHandler handler) {
    return ctx -> ctx.detach(asRunnable(ctx, handler));
  }

  @Override @Nonnull public Router group(@Nonnull Runnable action) {
    return newGroup("", action);
  }

  @Override @Nonnull public Router path(@Nonnull String pattern, @Nonnull Runnable action) {
    return newGroup(pattern, action);
  }

  @Override
  public Route route(@Nonnull String method, @Nonnull String pattern,
      @Nonnull Route.Handler handler) {
    /** Pattern: */
    StringBuilder pat = new StringBuilder();
    stack.forEach(it -> pat.append(it.pattern));
    pat.append(pattern);

    /** Filters: */
    List<Route.Filter> filters = stack.stream()
        .flatMap(Stack::toFilter)
        .collect(Collectors.toList());

    /** Before: */
    Route.Filter before = null;
    for (Route.Filter filter : filters) {
      before = before == null ? filter : before.then(filter);
    }

    /** Pipeline: */
    Route.Handler pipeline = before == null ? handler : before.then(handler);

    /** After: */
    Route.After after = stack.stream()
        .flatMap(Stack::toAfter)
        .reduce(null, (it, next) -> it == null ? next : it.then(next));

    /** Renderer: */
    Renderer renderer = stack.stream()
        .flatMap(Stack::toRenderer)
        .reduce(Renderer.TO_STRING, (it, next) -> next.then(it));

    /** Route: */
    RouteImpl route = new RouteImpl(method, pat.toString(), handler, pipeline.root(), after,
        renderer);
    String chipattern = basePath == null ? route.pattern() : basePath + route.pattern();
    if (method.equals("*")) {
      METHODS.forEach(m -> chi.insertRoute(m, chipattern, route));
    } else {
      chi.insertRoute(route.method(), chipattern, route);
    }
    routes.add(route);
    return route;
  }

  @Nonnull public Router start(@Nonnull Logger log) {
    if (err == null) {
      err = Route.ErrorHandler.DEFAULT;
    }
    this.rootErr = new RootErrorHandlerImpl(err, log, this::errorCode);
    this.stack.forEach(Stack::clear);
    this.stack = null;
    return this;
  }

  @Nonnull @Override public Route.RootErrorHandler errorHandler() {
    return rootErr;
  }

  @Nonnull @Override public Match match(@Nonnull Context ctx) {
    Match match = chi.findRoute(ctx, predicate, renderer, routers);
    // Set result and violate encapsulation :S
    ((BaseContext) ctx).prepare(match);
    return match;
  }

  @Nonnull @Override public Router errorCode(@Nonnull Class<? extends Throwable> type,
      @Nonnull StatusCode statusCode) {
    if (errorCodes == null) {
      errorCodes = new HashMap<>();
    }
    this.errorCodes.put(type.getCanonicalName(), statusCode);
    return this;
  }

  private StatusCode errorCode(@Nonnull Throwable x) {
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

  private Router newGroup(@Nonnull String pattern, @Nonnull Runnable action,
      Route.Filter... filter) {
    Stack stack = new Stack(pattern);
    Stream.of(filter).forEach(stack::then);
    this.stack.addLast(stack);
    if (action != null) {
      action.run();
    }
    this.stack.removeLast().clear();
    return this;
  }

  private static Runnable asRunnable(Context ctx, Route.Handler next) {
    return () -> next.root().apply(ctx);
  }
}
