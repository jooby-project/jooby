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

import io.jooby.AttributeMap;
import io.jooby.ErrorHandler;
import io.jooby.Jooby;
import io.jooby.Context;
import io.jooby.Err;
import io.jooby.ExecutionMode;
import io.jooby.MediaType;
import io.jooby.Parser;
import io.jooby.Renderer;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.RouterOptions;
import io.jooby.StatusCode;
import io.jooby.Throwing;
import io.jooby.ResponseHandler;
import io.jooby.internal.asm.ClassSource;
import io.jooby.internal.mvc.MvcCompiler;
import io.jooby.internal.mvc.MvcMetadata;
import io.jooby.internal.mvc.MvcMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
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

  private static final List<Class<? extends Annotation>> M_ANN = Arrays
      .asList(io.jooby.annotations.GET.class,
          io.jooby.annotations.POST.class,
          io.jooby.annotations.PUT.class,
          io.jooby.annotations.DELETE.class,
          io.jooby.annotations.PATCH.class,
          io.jooby.annotations.HEAD.class,
          io.jooby.annotations.OPTIONS.class);

  private ErrorHandler err;

  private Map<String, StatusCode> errorCodes;

  private RouterOptions options = new RouterOptions();

  private $Chi chi = new $Chi(options.isCaseSensitive(), options.isIgnoreTrailingSlash());

  private LinkedList<Stack> stack = new LinkedList<>();

  private List<Route> routes = new ArrayList<>();

  private CompositeRenderer renderer = new CompositeRenderer();

  private String basePath;

  private List<RadixTree> trees;

  private RouteAnalyzer analyzer;

  private Executor worker = new ForwardingExecutor();

  private Map<Route, Executor> routeExecutor = new HashMap<>();

  private Map<String, Parser> parsers = new HashMap<>();

  private AttributeMap attributes = new AttributeMap(new ConcurrentHashMap<>());

  private List<ResponseHandler> handlers = new ArrayList<>();

  private ClassSource source;

  public RouterImpl(ClassLoader loader) {
    this.source = new ClassSource(loader);
    this.analyzer = new RouteAnalyzer(source, false);
    stack.addLast(new Stack(""));

    defaultParser();
  }

  private void defaultParser() {
    parsers.put(MediaType.text.getValue(), Parser.RAW);
  }

  @Nonnull @Override public AttributeMap getAttributes() {
    return attributes;
  }

  @Nonnull @Override public Router setRouterOptions(@Nonnull RouterOptions options) {
    this.options = options;
    chi.setCaseSensitive(options.isCaseSensitive());
    chi.setIgnoreTrailingSlash(options.isIgnoreTrailingSlash());
    return this;
  }

  @Nonnull @Override public RouterOptions getRouterOptions() {
    return options;
  }

  @Nonnull @Override public Router setContextPath(@Nonnull String basePath) {
    if (routes.size() > 0) {
      throw new IllegalStateException("Base path must be set before adding any routes.");
    }
    this.basePath = normalizePath(basePath, options.isCaseSensitive(), true);
    return this;
  }

  @Nonnull @Override public Path getTmpdir() {
    return Paths.get(System.getProperty("java.io.tmpdir"));
  }

  @Nonnull @Override public String getContextPath() {
    return basePath == null ? "/" : basePath;
  }

  @Nonnull @Override public List<Route> getRoutes() {
    return routes;
  }

  @Nonnull @Override
  public Router use(@Nonnull Predicate<Context> predicate, @Nonnull Router router) {
    RadixTree tree = new $Chi(options.isCaseSensitive(), options.isIgnoreTrailingSlash())
        .with(predicate);
    if (trees == null) {
      trees = new ArrayList<>();
    }
    trees.add(tree);
    for (Route route : router.getRoutes()) {
      route(route.getMethod(), route.getPattern(), route.getHandler(), tree);
    }
    return this;
  }

  @Nonnull @Override public Router use(@Nonnull String path, @Nonnull Router router) {
    String prefix = normalizePath(path, options.isCaseSensitive(), true);
    if (prefix.equals("/")) {
      prefix = "";
    }
    for (Route route : router.getRoutes()) {
      String routePattern = prefix + route.getPattern();
      route(route.getMethod(), routePattern, route.getHandler());
    }
    return this;
  }

  @Nonnull @Override
  public Router use(@Nonnull Router router) {
    return use("", router);
  }

  @Nonnull @Override public Router mvc(@Nonnull Object router) {
    mvc(null, router.getClass(), () -> router);
    return this;
  }

  @Nonnull @Override public Router mvc(@Nonnull Class router) {
    throw new UnsupportedOperationException();
  }

  @Nonnull @Override
  public <T> Router mvc(@Nonnull Class<T> router, @Nonnull Provider<T> provider) {
    mvc(null, router, provider);
    return this;
  }

  @Nonnull @Override public Router renderer(@Nonnull Renderer renderer) {
    this.renderer.add(renderer);
    return this;
  }

  @Nonnull @Override
  public Router renderer(@Nonnull MediaType contentType, @Nonnull Renderer renderer) {
    return renderer(renderer.accept(contentType));
  }

  @Nonnull @Override public Router parser(@Nonnull MediaType contentType, @Nonnull Parser parser) {
    parsers.put(contentType.getValue(), parser);
    return this;
  }

  @Nonnull @Override public Executor getWorker() {
    return worker;
  }

  @Nonnull @Override public Router setWorker(Executor worker) {
    ForwardingExecutor workerRef = (ForwardingExecutor) this.worker;
    workerRef.executor = worker;
    return this;
  }

  @Nonnull @Override public Router setDefaultWorker(@Nonnull Executor worker) {
    ForwardingExecutor workerRef = (ForwardingExecutor) this.worker;
    if (workerRef.executor == null) {
      workerRef.executor = worker;
    }
    return this;
  }

  @Override @Nonnull public Router decorator(@Nonnull Route.Decorator decorator) {
    stack.peekLast().then(decorator);
    return this;
  }

  @Override @Nonnull public Router after(@Nonnull Route.After after) {
    stack.peekLast().then(after);
    return this;
  }

  @Nonnull @Override public Router before(@Nonnull Route.Before before) {
    return decorator(before);
  }

  @Nonnull @Override public Router error(@Nonnull ErrorHandler handler) {
    err = err == null ? handler : err.then(handler);
    return this;
  }

  @Nonnull @Override public Router dispatch(@Nonnull Runnable action) {
    return newStack(push().executor(worker), action);
  }

  @Nonnull @Override public Router dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    return newStack(push().executor(executor), action);
  }

  @Nonnull @Override public Router route(@Nonnull Runnable action) {
    return newStack("/", action);
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
    /** Make sure router options are in sync: */
    chi.setCaseSensitive(options.isCaseSensitive());
    chi.setIgnoreTrailingSlash(options.isIgnoreTrailingSlash());

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

    /** Route: */
    String safePattern = normalizePath(pat.toString(), options.isCaseSensitive(),
        options.isIgnoreTrailingSlash());
    Route route = new Route(method, safePattern, null, handler, pipeline, renderer, parsers);
    Stack stack = this.stack.peekLast();
    if (stack.executor != null) {
      routeExecutor.put(route, stack.executor);
    }
    String routePattern = normalizePath(basePath == null
        ? safePattern
        : basePath + safePattern, options.isCaseSensitive(), options.isIgnoreTrailingSlash());
    if (method.equals("*")) {
      METHODS.forEach(m -> tree.insert(m, routePattern, route));
    } else {
      tree.insert(route.getMethod(), routePattern, route);
    }
    routes.add(route);

    if (handler instanceof Route.Aware) {
      ((Route.Aware) handler).setRoute(route);
    }
    return route;
  }

  @Nonnull public Router start(@Nonnull Jooby owner) {
    if (err == null) {
      err = ErrorHandler.log(owner.getLog(), StatusCode.NOT_FOUND)
          .then(ErrorHandler.DEFAULT);
    }
    ExecutionMode mode = owner.getExecutionMode();
    for (Route route : routes) {
      Executor executor = routeExecutor.get(route);
      if (executor instanceof ForwardingExecutor) {
        executor = ((ForwardingExecutor) executor).executor;
      }
      /** Return type: */
      if (route.getReturnType() == null) {
        route.setReturnType(analyzer.returnType(route.getHandle()));
      }
      Route.Handler pipeline = Pipeline
          .compute(source.getLoader(), route, mode, executor, handlers);
      route.setPipeline(pipeline);
    }
    // unwrap executor
    worker = ((ForwardingExecutor) worker).executor;
    this.stack.forEach(Stack::clear);
    this.stack = null;
    routeExecutor.clear();
    routeExecutor = null;
    source.destroy();
    source = null;
    return this;
  }

  @Override public Logger getLog() {
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

  @Nonnull @Override public ErrorHandler getErrorHandler() {
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
      return ((Err) x).getStatusCode();
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

  @Nonnull @Override public Router responseHandler(ResponseHandler handler) {
    handlers.add(handler);
    return this;
  }

  @Override public String toString() {
    StringBuilder buff = new StringBuilder();
    int size = IntStream.range(0, routes.size())
        .map(i -> routes.get(i).getMethod().length() + 1)
        .max()
        .orElse(0);

    routes.forEach(
        r -> buff.append(String.format("\n  %-" + size + "s", r.getMethod()))
            .append(r.getPattern()));
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
    Stack stack = new Stack(normalizePath(pattern, options.isCaseSensitive(), true));
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

  private void mvc(String prefix, Class type, Provider provider) {
    find(prefix, type, method -> {

      Route.Handler instance = MvcCompiler.newHandler(source.getLoader(), method, provider);

      route(method.getHttpMethod(), method.getPattern(), instance)
          .setReturnType(method.getReturnType(source.getLoader()));
    });
  }

  private void find(String prefix, Class type, Throwing.Consumer<MvcMethod> consumer) {
    String classPath = prefix == null ? path(type) : prefix + "/" + path(type);
    MvcMetadata mvcMetadata = new MvcMetadata(source);
    mvcMetadata.parse(type);
    List<MvcMethod> routes = new ArrayList<>();
    Stream.of(type.getDeclaredMethods())
        .forEach(method -> {
          if (Modifier.isPublic(method.getModifiers())) {
            toHttpMethod(method).forEach(httpMethod -> {
              MvcMethod mvc = mvcMetadata.get(method);
              mvc.setPattern(classPath + "/" + path(method));
              mvc.setMethod(method);
              mvc.setHttpMethod(httpMethod);
              routes.add(mvc);
            });
          }
        });
    Collections.sort(routes, Comparator.comparingInt(MvcMethod::getLine));
    routes.forEach(consumer);
    mvcMetadata.destroy();
  }

  private List<String> toHttpMethod(Method method) {
    List<String> result = new ArrayList<>();
    for (Class<? extends Annotation> m : M_ANN) {
      Annotation annotation = method.getAnnotation(m);
      if (annotation != null) {
        result.add(annotation.annotationType().getSimpleName());
      }
    }
    if (result.size() == 0) {
      if (method.getAnnotation(io.jooby.annotations.Path.class) != null) {
        result.add("GET");
      }
    }
    return result;
  }

  private String path(AnnotatedElement type) {
    io.jooby.annotations.Path path = type.getAnnotation(io.jooby.annotations.Path.class);
    return path == null ? "" : path.value()[0];
  }
}
