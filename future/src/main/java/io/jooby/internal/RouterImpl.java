package io.jooby.internal;

import io.jooby.After;
import io.jooby.Before;
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
import java.util.Collections;
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
      return filters.stream();
    }
  }

  private static final Integer mGET = Integer.valueOf(1);
  private static final Integer mPOST = Integer.valueOf(2);
  private static final Integer mPUT = Integer.valueOf(3);
  private static final Integer mDELETE = Integer.valueOf(4);
  private static final Integer mPATCH = Integer.valueOf(5);

  private static final Integer mHEAD = Integer.valueOf(6);
  private static final Integer mOPTIONS = Integer.valueOf(7);
  private static final Integer mCONNECT = Integer.valueOf(8);
  private static final Integer mTRACE = Integer.valueOf(9);

  private ErrorHandler err;

  private Map<String, StatusCode> errorCodes;

  private final $Chi chi = new $Chi();

  private LinkedList<Stack> stack = new LinkedList<>();

  private List<Route> routes = new ArrayList<>();

  public RouterImpl() {
    stack.addLast(new Stack(""));
  }

  @Nonnull @Override public Router renderer(@Nonnull Renderer renderer) {
    stack.peekLast().then(renderer);
    return this;
  }

  @Override @Nonnull public Router filter(@Nonnull Filter filter) {
    stack.peekLast().then(filter);
    return this;
  }

  @Override @Nonnull public Router after(@Nonnull After after) {
    return filter(after);
  }

  @Nonnull @Override public Router before(@Nonnull Before before) {
    return filter(before);
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
    /** Before: */
    List<Filter> before = filters.stream()
        .filter(Before.class::isInstance)
        .collect(Collectors.toList());
    /** Renderer & After: */
    List<Filter> after = filters.stream()
        .filter(After.class::isInstance)
        .collect(Collectors.toList());
    /** Default Renderer: */
    if (!after.stream().anyMatch(Renderer.class::isInstance)) {
      after.add(0, Renderer.TO_STRING);
    }
    /** Handler: */
    Handler h = after.stream().skip(1)
        .reduce(after.get(0), Filter::then)
        .then(handler);
    if (before.size() > 0) {
      h = before.stream().skip(1)
          .reduce(before.get(0), Filter::then)
          .then(h);
    }
    /** Route: */
    RouteImpl route = new RouteImpl(method, pat.toString(), h);
    if (method.equals("*")) {
      METHODS.forEach(m -> chi.insertRoute(methodCode(m), route.pattern(), route));
    } else {
      chi.insertRoute(methodCode(route.method()), route.pattern(), route);
    }
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

  @Override public Route match(String method, String path) {
    return chi.findRoute(methodCode(method), method, path);
  }

  @Nonnull @Override
  public Router errorCode(@Nonnull Class<? extends Throwable> type,
      @Nonnull StatusCode statusCode) {
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

  private Integer methodCode(String method) {
    if (GET.equals(method)) {
      return mGET;
    }
    if (POST.equals(method)) {
      return mPOST;
    }
    if (PUT.equals(method)) {
      return mPUT;
    }
    if (DELETE.equals(method)) {
      return mDELETE;
    }
    if (PATCH.equals(method)) {
      return mPATCH;
    }
    if (PATCH.equals(method)) {
      return mPATCH;
    }
    if (HEAD.equals(method)) {
      return mHEAD;
    }
    if (OPTIONS.equals(method)) {
      return mOPTIONS;
    }
    if (CONNECT.equals(method)) {
      return mCONNECT;
    }
    if (TRACE.equals(method)) {
      return mTRACE;
    }
    throw new IllegalArgumentException("Unknown method: " + method);
  }
}
