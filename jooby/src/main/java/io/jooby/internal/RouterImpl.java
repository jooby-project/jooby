/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.HeadHandler;
import io.jooby.RegistryException;
import io.jooby.ServiceKey;
import io.jooby.StatusCodeException;
import io.jooby.ErrorHandler;
import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.MessageDecoder;
import io.jooby.MessageEncoder;
import io.jooby.ResponseHandler;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.RouterOptions;
import io.jooby.ServiceRegistry;
import io.jooby.SessionOptions;
import io.jooby.StatusCode;
import io.jooby.SneakyThrows;
import io.jooby.TemplateEngine;
import io.jooby.annotations.Dispatch;
import io.jooby.internal.asm.ClassSource;
import io.jooby.internal.mvc.MvcAnnotationParser;
import io.jooby.internal.mvc.MvcCompiler;
import io.jooby.internal.mvc.MvcMetadata;
import io.jooby.internal.mvc.MvcMethod;
import io.jooby.internal.mvc.MvcAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
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
    private List<Route.Decorator> decoratorList = new ArrayList<>();
    private List<Route.Before> beforeList = new ArrayList<>();
    private List<Route.After> afterList = new ArrayList<>();

    public Stack(String pattern) {
      this.pattern = pattern;
    }

    public void then(Route.Decorator filter) {
      decoratorList.add(filter);
    }

    public void then(Route.After after) {
      afterList.add(after);
    }

    public void then(Route.Before before) {
      beforeList.add(before);
    }

    public Stream<Route.Decorator> toDecorator() {
      return decoratorList.stream();
    }

    public Stream<Route.After> toAfter() {
      return afterList.stream();
    }

    public Stream<Route.Before> toBefore() {
      return beforeList.stream();
    }

    public void clear() {
      this.decoratorList.clear();
      this.afterList.clear();
      this.beforeList.clear();
      executor = null;
    }

    public Stack executor(Executor executor) {
      this.executor = executor;
      return this;
    }
  }

  private ErrorHandler err;

  private Map<String, StatusCode> errorCodes;

  private RouterOptions options = new RouterOptions();

  private RadixTree chi = new $Chi();

  private LinkedList<Stack> stack = new LinkedList<>();

  private List<Route> routes = new ArrayList<>();

  private CompositeMessageEncoder renderer = new CompositeMessageEncoder();

  private String basePath;

  private List<RadixTree> trees;

  private RouteAnalyzer analyzer;

  private Executor worker = new ForwardingExecutor();

  private Map<Route, Executor> routeExecutor = new HashMap<>();

  private Map<String, MessageDecoder> parsers = new HashMap<>();

  private Map<String, Object> attributes = new ConcurrentHashMap<>();

  private List<ResponseHandler> handlers = new ArrayList<>();

  private ServiceRegistry services = new ServiceRegistryImpl();

  private MvcAnnotationParser annotationParser;

  private ClassSource source;

  private SessionOptions sessionOptions = new SessionOptions();

  private String flashName = "jooby.flash";

  public RouterImpl(ClassLoader loader) {
    this.source = new ClassSource(loader);
    this.analyzer = new RouteAnalyzer(source, false);
    stack.addLast(new Stack(""));

    defaultParser();
  }

  private void defaultParser() {
    parsers.put(MediaType.text.getValue(), MessageDecoder.RAW);
  }

  @Nonnull @Override public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Nonnull @Override public Router setRouterOptions(@Nonnull RouterOptions options) {
    this.options = options;
    return this;
  }

  @Nonnull @Override public RouterOptions getRouterOptions() {
    return options;
  }

  @Nonnull @Override public Router setContextPath(@Nonnull String basePath) {
    if (routes.size() > 0) {
      throw new IllegalStateException("Base path must be set before adding any routes.");
    }
    this.basePath = normalizePath(basePath, false, true);
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
    RadixTree tree = new $Chi().with(predicate);
    if (trees == null) {
      trees = new ArrayList<>();
    }
    trees.add(tree);
    for (Route route : router.getRoutes()) {
      Route newRoute = defineRoute(route.getMethod(), route.getPattern(), route.getHandler(), tree);
      copy(route, newRoute);
    }
    return this;
  }

  @Nonnull @Override public Router use(@Nonnull String path, @Nonnull Router router) {
    String prefix = normalizePath(path, false, true);
    if (prefix.equals("/")) {
      prefix = "";
    }
    for (Route route : router.getRoutes()) {
      String routePattern = prefix + route.getPattern();
      Route newRoute = defineRoute(route.getMethod(), routePattern, route.getHandler(), chi);
      copy(route, newRoute);
    }
    return this;
  }

  @Nonnull @Override
  public Router use(@Nonnull Router router) {
    return use("", router);
  }

  @Nonnull @Override public Router mvc(@Nonnull Object router) {
    checkMvcAnnotations();
    mvc(null, router.getClass(), () -> router);
    return this;
  }

  @Nonnull @Override public Router mvc(@Nonnull Class router) {
    throw new UnsupportedOperationException();
  }

  @Nonnull @Override
  public <T> Router mvc(@Nonnull Class<T> router, @Nonnull Provider<T> provider) {
    checkMvcAnnotations();
    mvc(null, router, provider);
    return this;
  }

  @Nonnull @Override public Router encoder(@Nonnull MessageEncoder encoder) {
    this.renderer.add(encoder);
    return this;
  }

  @Nonnull @Override
  public Router encoder(@Nonnull MediaType contentType, @Nonnull MessageEncoder encoder) {
    if (encoder instanceof TemplateEngine) {
      // Mime-Type is ignored for TemplateEngine due they depends on specific object type and file
      // extension.
      return encoder(encoder);
    }
    return encoder(encoder.accept(contentType));
  }

  @Nonnull @Override public Router decoder(@Nonnull MediaType contentType, @Nonnull
      MessageDecoder decoder) {
    parsers.put(contentType.getValue(), decoder);
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
    stack.peekLast().then(before);
    return this;
  }

  @Nonnull @Override public Router error(@Nonnull ErrorHandler handler) {
    err = err == null ? handler : err.then(handler);
    return this;
  }

  @Nonnull @Override public Router dispatch(@Nonnull Runnable body) {
    return newStack(push().executor(worker), body);
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

  @Nonnull @Override public SessionOptions getSessionOptions() {
    return sessionOptions;
  }

  @Nonnull @Override public Router setSessionOptions(SessionOptions sessionOptions) {
    this.sessionOptions = sessionOptions;
    return this;
  }

  @Override
  public Route route(@Nonnull String method, @Nonnull String pattern,
      @Nonnull Route.Handler handler) {
    return defineRoute(method, pattern, handler, chi);
  }

  private Route defineRoute(@Nonnull String method, @Nonnull String pattern,
      @Nonnull Route.Handler handler, RadixTree tree) {
    /** Pattern: */
    StringBuilder patternBuff = new StringBuilder();
    stack.stream().filter(it -> it.pattern != null).forEach(it -> patternBuff.append(it.pattern));
    patternBuff.append(pattern);

    /** Before: */
    Route.Before before = stack.stream()
        .flatMap(Stack::toBefore)
        .reduce(null, (it, next) -> it == null ? next : it.then(next));

    /** Decorator: */
    List<Route.Decorator> decoratorList = stack.stream()
        .flatMap(Stack::toDecorator)
        .collect(Collectors.toList());
    Route.Decorator decorator = decoratorList.stream()
        .reduce(null, (it, next) -> it == null ? next : it.then(next));

    /** After: */
    Route.After after = stack.stream()
        .flatMap(Stack::toAfter)
        .reduce(null, (it, next) -> it == null ? next : it.then(next));

    /** Route: */
    String safePattern = Router.normalizePath(patternBuff.toString(), false, true);
    Route route = new Route(method, safePattern, handler);
    route.setPathKeys(Router.pathKeys(safePattern));
    route.setBefore(before);
    route.setAfter(after);
    route.setDecorator(decorator);
    route.setEncoder(renderer);
    route.setDecoders(parsers);

    decoratorList.forEach(it -> it.setRoute(route));
    handler.setRoute(route);

    Stack stack = this.stack.peekLast();
    if (stack.executor != null) {
      routeExecutor.put(route, stack.executor);
    }
    String routePattern = normalizePath(basePath == null
        ? safePattern
        : basePath + safePattern, false, true);
    tree.insert(route.getMethod(), routePattern, route);
    if (route.isHttpOptions()) {
      tree.insert(Router.OPTIONS, routePattern, route);
    } else if (route.isHttpTrace()) {
      tree.insert(Router.TRACE, routePattern, route);
    } else if (route.isHttpHead() && route.getMethod().equals(GET)) {
      tree.insert(Router.HEAD, routePattern, route);
    }
    routes.add(route);

    return route;
  }

  @Nonnull public Router start(@Nonnull Jooby owner) {
    if (err == null) {
      err = ErrorHandler.DEFAULT;
    } else {
      err = err.then(ErrorHandler.DEFAULT);
    }
    renderer.add(MessageEncoder.TO_STRING);
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

      /** Response handler: */
      Route.Handler pipeline = Pipeline
          .compute(source.getLoader(), route, mode, executor, handlers);
      route.setPipeline(pipeline);
      /** Final render */
      route.setEncoder(renderer);
    }
    // router options
    if (options.getIgnoreCase() || options.getIgnoreTrailingSlash()) {
      chi = chi.options(options.getIgnoreCase(), options.getIgnoreTrailingSlash());
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

  @Nonnull @Override public Router executor(@Nonnull String name, @Nonnull Executor executor) {
    services.put(ServiceKey.key(Executor.class, name), executor);
    return this;
  }

  public void destroy() {
    routes.clear();
    routes = null;
    chi.destroy();
    if (errorCodes != null) {
      errorCodes.clear();
      errorCodes = null;
    }
    if (this.trees != null) {
      this.trees.forEach(RadixTree::destroy);
      this.trees.clear();
      this.trees = null;
    }
  }

  @Nonnull @Override public ErrorHandler getErrorHandler() {
    return err;
  }

  @Nonnull @Override public Match match(@Nonnull Context ctx) {
    return chi.find(ctx, ctx.pathString(), renderer, trees);
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
    if (x instanceof StatusCodeException) {
      return ((StatusCodeException) x).getStatusCode();
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

  @Nonnull @Override public ServiceRegistry getServices() {
    return services;
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type, @Nonnull String name)
      throws RegistryException {
    return services.require(type, name);
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type) throws RegistryException {
    return services.require(type);
  }

  @Nonnull @Override public <T> T require(@Nonnull ServiceKey<T> key) throws RegistryException {
    return services.require(key);
  }

  @Nonnull @Override public String getFlashCookie() {
    return flashName;
  }

  @Nonnull @Override public Router setFlashCookie(@Nonnull String name) {
    this.flashName = name;
    return this;
  }

  @Override public String toString() {
    StringBuilder buff = new StringBuilder();
    if (routes != null) {
      int size = IntStream.range(0, routes.size())
          .map(i -> routes.get(i).getMethod().length() + 1)
          .max()
          .orElse(0);

      routes.forEach(
          r -> buff.append(String.format("\n  %-" + size + "s", r.getMethod()))
              .append(r.getPattern()));
    }
    return buff.length() > 0 ? buff.substring(1) : "";
  }

  private Router newStack(@Nonnull String pattern, @Nonnull Runnable action,
      Route.Decorator... decorator) {
    return newStack(push(pattern), action, decorator);
  }

  private Stack push() {
    return new Stack(null);
  }

  private Stack push(String pattern) {
    Stack stack = new Stack(normalizePath(pattern, false, true));
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

      Route.Handler instance = MvcCompiler
          .newHandler(source.getLoader(), method, provider);

      Route route = route(method.getHttpMethod(), method.getPattern(), instance)
          .setReturnType(method.getReturnType(source.getLoader()));

      MvcAnnotation model = method.getModel();
      List<MediaType> produces = model.getProduces();
      if (produces.size() > 0) {
        route.setProduces(produces);
      }

      List<MediaType> consumes = model.getConsumes();
      if (consumes.size() > 0) {
        route.setConsumes(consumes);
      }
    });
  }

  private void find(String prefix, Class type, SneakyThrows.Consumer<MvcMethod> consumer) {
    MvcMetadata mvcMetadata = new MvcMetadata(source);
    mvcMetadata.parse(type);
    List<MvcMethod> routes = new ArrayList<>();
    Stream.of(type.getDeclaredMethods()).forEach(method -> {
      if (Modifier.isPublic(method.getModifiers())) {
        annotationParser.parse(method).forEach(model -> {
          String[] paths = pathPrefix(prefix, model.getPath());
          for (String path : paths) {
            MvcMethod mvc = mvcMetadata.create(method);
            mvc.setPattern(path);
            mvc.setModel(model);
            mvc.setMethod(method);
            routes.add(mvc);
          }
        });
      }
    });
    Collections.sort(routes, Comparator.comparingInt(MvcMethod::getLine));
    routes.forEach(mvc -> {
      String executorKey = dispatchTo(mvc.getMethod());
      if (executorKey != null) {
        if (executorKey.length() == 0) {
          dispatch(() -> consumer.accept(mvc));
        } else {
          Executor executor = services.getOrNull(ServiceKey.key(Executor.class, executorKey));
          if (executor == null) {
            // TODO: replace with usage exception
            throw new IllegalArgumentException(
                "Missing executor: " + executorKey + ", required by: " + mvc.getMethod()
                    .getDeclaringClass().getName() + "." + mvc.getMethod()
                    .getName() + ", at line: " + mvc.getLine());
          }
          dispatch(executor, () -> consumer.accept(mvc));
        }
      } else {
        consumer.accept(mvc);
      }
    });
    mvcMetadata.destroy();
  }

  private String dispatchTo(Method method) {
    Dispatch dispatch = method.getAnnotation(Dispatch.class);
    if (dispatch != null) {
      return dispatch.value();
    }
    Dispatch parent = method.getDeclaringClass().getAnnotation(Dispatch.class);
    if (parent != null) {
      return parent.value();
    }
    return null;
  }

  private String[] pathPrefix(String prefix, String[] path) {
    if (prefix == null) {
      return path;
    }
    String[] result = new String[path.length];
    for (int i = 0; i < path.length; i++) {
      result[i] = prefix + "/" + path[i];
    }
    return result;
  }

  private void checkMvcAnnotations() {
    if (annotationParser == null) {
      annotationParser = MvcAnnotationParser.create(source.getLoader());
    }
  }

  private void copy(Route src, Route it) {
    Route.Before before = Optional.ofNullable(it.getBefore())
        .map(filter -> Optional.ofNullable(src.getBefore()).map(filter::then).orElse(filter))
        .orElseGet(src::getBefore);

    Route.Decorator decorator = Optional.ofNullable(it.getDecorator())
        .map(filter -> Optional.ofNullable(src.getDecorator()).map(filter::then).orElse(filter))
        .orElseGet(src::getDecorator);

    Route.After after = Optional.ofNullable(it.getAfter())
        .map(filter -> Optional.ofNullable(src.getAfter()).map(filter::then).orElse(filter))
        .orElseGet(src::getAfter);

    it.setBefore(before);
    it.setDecorator(decorator);
    it.setAfter(after);

    it.setConsumes(src.getConsumes());
    it.setProduces(src.getProduces());
    it.setHandle(src.getHandle());
  }
}
