/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.App;
import io.jooby.BaseContext;
import io.jooby.Context;
import io.jooby.Err;
import io.jooby.ExecutionMode;
import io.jooby.Renderer;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.Usage;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private boolean gzip;
    private Executor executor;
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

    public Stack gzip(boolean gzip) {
      this.gzip = gzip;
      return this;
    }

    public void clear() {
      this.filters.clear();
      this.renderers.clear();
      this.afters.clear();
      executor = null;
    }

    public Stack executor(Executor executor) {
      this.executor = executor;
      return this;
    }
  }

  private static final Executor LAZY_WORKER = task -> {
    throw new Usage("Worker executor is not ready.");
  };

  private Route.ErrorHandler err;

  private Route.RootErrorHandler rootErr;

  private Map<String, StatusCode> errorCodes;

  private RadixTree chi = new $Chi();

  private LinkedList<Stack> stack = new LinkedList<>();

  private List<Route> routes = new ArrayList<>();

  private Renderer renderer = Renderer.TO_STRING;

  private Map<String, Executor> executors = new HashMap<>();

  private String basePath = "";

  private List<RadixTree> trees;

  private RouteAnalyzer analyzer;

  private Executor worker;

  public RouterImpl(RouteAnalyzer analyzer) {
    this.analyzer = analyzer;
    stack.addLast(new Stack(""));
  }

  @Nonnull @Override public Router basePath(@Nonnull String basePath) {
    if (routes.size() > 0) {
      throw new IllegalStateException("Base path must be set before adding any routes.");
    }
    this.basePath = basePath;
    return this;
  }

  @Nonnull @Override public Path tmpdir() {
    return Paths.get(System.getProperty("java.io.tmpdir"));
  }

  @Nonnull @Override public String basePath() {
    return basePath;
  }

  @Nonnull @Override public List<Route> routes() {
    return routes;
  }

  @Nonnull @Override
  public Router use(@Nonnull Predicate<Context> predicate, @Nonnull Router router) {
    RadixTree tree = new $Chi().with(predicate);
    if (trees == null) {
      trees = new ArrayList<>();
    }
    trees.add(tree);
    for (Route route : router.routes()) {
      route(route.method(), route.pattern(), route.handler(), tree);
    }
    return this;
  }

  @Nonnull @Override public Router use(@Nonnull String path, @Nonnull Router router) {
    for (Route route : router.routes()) {
      route(route.method(), path + route.pattern(), route.handler());
    }
    return this;
  }

  @Nonnull @Override
  public Router use(@Nonnull Router router) {
    return use("", router);
  }

  @Nonnull @Override public Router renderer(@Nonnull Renderer renderer) {
    this.renderer = renderer.then(this.renderer);
    stack.peekLast().then(renderer);
    return this;
  }

  @Nonnull @Override public Executor worker() {
    return worker;
  }

  @Nonnull @Override public Router worker(Executor worker) {
    this.worker = worker;
    return this;
  }

  @Nonnull @Override public Router gzip(@Nonnull Runnable action) {
    return newStack(push().gzip(true), action);
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

  @Override @Nonnull public Router stack(@Nonnull Runnable action) {
    return newStack("", action);
  }

  @Nonnull @Override public Router stack(@Nonnull Executor executor, @Nonnull Runnable action) {
    return newStack(push().executor(executor == null ? LAZY_WORKER : executor), action);
  }

  @Override @Nonnull public Router path(@Nonnull String pattern, @Nonnull Runnable action) {
    return newStack(pattern, action);
  }

  @Override
  public Route route(@Nonnull String method, @Nonnull String pattern,
      @Nonnull Route.Handler handler) {
    return route(method, pattern, handler, chi);
  }

  private Route route(@Nonnull String method, @Nonnull String pattern,
      @Nonnull Route.Handler handler, RadixTree tree) {
    /** Pattern: */
    StringBuilder pat = new StringBuilder();
    stack.stream().filter(it -> it.pattern != null).forEach(it -> pat.append(it.pattern));
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

    if (after != null) {
      pipeline = pipeline.then(after);
    }
    /** Renderer: */
    Renderer renderer = stack.stream()
        .flatMap(Stack::toRenderer)
        .reduce(Renderer.TO_STRING, (it, next) -> next.then(it));

    /** Return type: */
    Type returnType = analyzer.returnType(handler);

    /** Route: */
    RouteImpl route = new RouteImpl(method, pat.toString(), returnType, handler, pipeline,
        renderer);
    Stack stack = this.stack.peekLast();
    route.gzip(stack.gzip);
    route.executor(stack.executor);
    String finalpattern = basePath == null ? route.pattern() : basePath + route.pattern();
    if (method.equals("*")) {
      METHODS.forEach(m -> tree.insert(m, finalpattern, route));
    } else {
      tree.insert(route.method(), finalpattern, route);
    }
    routes.add(route);
    return route;
  }

  @Nonnull public Router start(@Nonnull App owner) {
    if (err == null) {
      err = Route.ErrorHandler.DEFAULT;
    }
    ExecutionMode mode = owner.mode();
    for (Route route : routes) {
      RouteImpl routeImpl = (RouteImpl) route;
      Executor executor = routeImpl.executor();
      if (executor == LAZY_WORKER) {
        routeImpl.executor(owner.worker());
      }
      Route.Handler pipeline = Pipeline.compute(analyzer.getClassLoader(), routeImpl, mode);
      routeImpl.pipeline(pipeline);
    }
    this.rootErr = new RootErrorHandlerImpl(err, owner.log(), this::errorCode);
    this.stack.forEach(Stack::clear);
    this.stack = null;
    return this;
  }

  public void destroy() {
    routes.clear();
    routes = null;
    chi.destroy();
    chi = null;
    if (errorCodes != null) {
      errorCodes.clear();
      errorCodes = null;
    }
    if (this.trees != null) {
      this.trees.forEach(RadixTree::destroy);
      this.trees.clear();
      this.trees = null;
    }
    this.rootErr = null;
    // NOOP
  }

  @Nonnull @Override public Route.RootErrorHandler errorHandler() {
    return rootErr;
  }

  @Nonnull @Override public Match match(@Nonnull Context ctx) {
    Match match = chi.find(ctx, renderer, trees);
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
    return buff.length() > 0 ? buff.substring(1) : "";
  }

  private Router newStack(@Nonnull String pattern, @Nonnull Runnable action,
      Route.Filter... filter) {
    return newStack(push(pattern), action, filter);
  }

  private Stack push() {
    return new Stack(null);
  }

  private Stack push(String pattern) {
    Stack stack = new Stack(pattern);
    if (this.stack.size() > 0) {
      Stack parent = this.stack.getLast();
      stack.gzip(parent.gzip);
      stack.executor = parent.executor;
    }
    return stack;
  }

  private Router newStack(@Nonnull Stack stack, @Nonnull Runnable action,
      Route.Filter... filter) {
    Stream.of(filter).forEach(stack::then);
    this.stack.addLast(stack);
    if (action != null) {
      action.run();
    }
    this.stack.removeLast().clear();
    return this;
  }
}
