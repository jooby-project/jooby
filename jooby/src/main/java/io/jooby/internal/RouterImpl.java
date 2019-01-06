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

import io.jooby.ErrorHandler;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.Context;
import io.jooby.Err;
import io.jooby.ExecutionMode;
import io.jooby.MediaType;
import io.jooby.Parser;
import io.jooby.Renderer;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.jooby.Router.normalizePath;

public class RouterImpl implements Router {

  private static class Stack {
    private String pattern;
    private Executor executor;
    private List<Route.Decorator> filters = new ArrayList<>();
    private List<Renderer> renderers = new ArrayList<>();
    private List<Route.After> afters = new ArrayList<>();

    public Stack(String pattern) {
      this.pattern = pattern;
    }

    public void then(Renderer renderer) {
      renderers.add(renderer);
    }

    public void then(Route.Decorator filter) {
      filters.add(filter);
    }

    public void then(Route.After after) {
      afters.add(after);
    }

    public Stream<Route.Decorator> toFilter() {
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

  private ErrorHandler err;

  private Map<String, StatusCode> errorCodes;

  private boolean caseSensitive = false;

  private boolean ignoreTrailingSlash = true;

  private $Chi chi = new $Chi(caseSensitive, ignoreTrailingSlash);

  private LinkedList<Stack> stack = new LinkedList<>();

  private List<Route> routes = new ArrayList<>();

  private CompositeRenderer renderer = new CompositeRenderer();

  private String basePath;

  private List<RadixTree> trees;

  private RouteAnalyzer analyzer;

  private Executor worker;

  private Map<String, Parser> parsers = new HashMap<>();

  public RouterImpl(RouteAnalyzer analyzer) {
    this.analyzer = analyzer;
    stack.addLast(new Stack(""));
  }

  @Nonnull @Override public Router caseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    chi.setCaseSensitive(caseSensitive);
    return this;
  }

  @Nonnull @Override public Router ignoreTrailingSlash(boolean ignoreTrailingSlash) {
    this.ignoreTrailingSlash = ignoreTrailingSlash;
    chi.setIgnoreTrailingSlash(ignoreTrailingSlash);
    return this;
  }

  @Nonnull @Override public Router basePath(@Nonnull String basePath) {
    if (routes.size() > 0) {
      throw new IllegalStateException("Base path must be set before adding any routes.");
    }
    this.basePath = normalizePath(basePath, caseSensitive, true);
    return this;
  }

  @Nonnull @Override public Path tmpdir() {
    return Paths.get(System.getProperty("java.io.tmpdir"));
  }

  @Nonnull @Override public String basePath() {
    return basePath == null ? "/": basePath;
  }

  @Nonnull @Override public List<Route> routes() {
    return routes;
  }

  @Nonnull @Override
  public Router use(@Nonnull Predicate<Context> predicate, @Nonnull Router router) {
    RadixTree tree = new $Chi(caseSensitive, ignoreTrailingSlash).with(predicate);
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
    String prefix = normalizePath(path, caseSensitive, true);
    if (prefix.equals("/")) {
      prefix = "";
    }
    for (Route route : router.routes()) {
      String routePattern = prefix + route.pattern();
      route(route.method(), routePattern, route.handler());
    }
    return this;
  }

  @Nonnull @Override
  public Router use(@Nonnull Router router) {
    return use("", router);
  }

  @Nonnull @Override public Router renderer(@Nonnull Renderer renderer) {
    this.renderer.add(renderer);
    return this;
  }

  @Nonnull @Override
  public Router renderer(@Nonnull String contentType, @Nonnull Renderer renderer) {
    return renderer(renderer.accept(contentType));
  }

  @Nonnull @Override public Router parser(@Nonnull String contentType, @Nonnull Parser parser) {
    parsers.put(contentType, parser);
    return this;
  }

  @Nonnull @Override public Executor worker() {
    return worker;
  }

  @Nonnull @Override public Router worker(Executor worker) {
    this.worker = worker;
    return this;
  }

  @Override @Nonnull public Router decorate(@Nonnull Route.Decorator decorator) {
    stack.peekLast().then(decorator);
    return this;
  }

  @Override @Nonnull public Router after(@Nonnull Route.After after) {
    stack.peekLast().then(after);
    return this;
  }

  @Nonnull @Override public Router before(@Nonnull Route.Before before) {
    return decorate(before);
  }

  @Nonnull @Override public Router error(@Nonnull ErrorHandler handler) {
    err = err == null ? handler : err.then(handler);
    return this;
  }

  @Override @Nonnull public Router group(@Nonnull Runnable action) {
    return newStack(push(), action);
  }

  @Nonnull @Override public Router group(@Nonnull Executor executor, @Nonnull Runnable action) {
    return newStack(push().executor(executor == null ? LAZY_WORKER : executor), action);
  }

  @Override @Nonnull public Router group(@Nonnull String pattern, @Nonnull Runnable action) {
    return newStack(pattern, action);
  }

  @Nonnull @Override public Router group(@Nonnull Executor executor, @Nonnull String pattern,
      @Nonnull Runnable action) {
    return newStack(push(pattern).executor(executor == null ? LAZY_WORKER : executor), action);
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

    /** Decorators: */
    List<Route.Decorator> filters = stack.stream()
        .flatMap(Stack::toFilter)
        .collect(Collectors.toList());

    /** Before: */
    Route.Decorator before = null;
    for (Route.Decorator filter : filters) {
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

    /** Return type: */
    Type returnType = analyzer.returnType(handler);

    /** Route: */
    String safePattern = pat.toString();
    List<String> pathKeys = Router.pathKeys(safePattern);
    RouteImpl route = new RouteImpl(method, safePattern, pathKeys, returnType, handler, pipeline,
        renderer, parsers);
    Stack stack = this.stack.peekLast();
    route.executor(stack.executor);
    String routePattern = normalizePath(basePath == null ? safePattern : basePath + safePattern, caseSensitive, ignoreTrailingSlash);
    if (method.equals("*")) {
      METHODS.forEach(m -> tree.insert(m, routePattern, route));
    } else {
      tree.insert(route.method(), routePattern, route);
    }
    routes.add(route);
    return route;
  }

  @Nonnull public Router start(@Nonnull Jooby owner) {
    if (err == null) {
      err = ErrorHandler.log(owner.log(), StatusCode.NOT_FOUND)
          .then(ErrorHandler.DEFAULT);
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
    this.stack.forEach(Stack::clear);
    this.stack = null;
    return this;
  }

  @Override public Logger log() {
    return LoggerFactory.getLogger(getClass());
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
    // NOOP
  }

  @Nonnull @Override public ErrorHandler errorHandler() {
    return err;
  }

  @Nonnull @Override public Match match(@Nonnull Context ctx) {
    return chi.find(ctx, renderer, trees);
  }

  @Nonnull @Override public Router errorCode(@Nonnull Class<? extends Throwable> type,
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
    return buff.length() > 0 ? buff.substring(1) : "";
  }

  private Router newStack(@Nonnull String pattern, @Nonnull Runnable action,
      Route.Decorator... filter) {
    return newStack(push(pattern), action, filter);
  }

  private Stack push() {
    return new Stack(null);
  }

  private Stack push(String pattern) {
    Stack stack = new Stack(normalizePath(pattern, caseSensitive, true));
    if (this.stack.size() > 0) {
      Stack parent = this.stack.getLast();
      stack.executor = parent.executor;
    }
    return stack;
  }

  private Router newStack(@Nonnull Stack stack, @Nonnull Runnable action,
      Route.Decorator... filter) {
    Stream.of(filter).forEach(stack::then);
    this.stack.addLast(stack);
    if (action != null) {
      action.run();
    }
    this.stack.removeLast().clear();
    return this;
  }
}
