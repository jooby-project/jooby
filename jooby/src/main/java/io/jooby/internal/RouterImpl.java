/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import com.typesafe.config.Config;
import io.jooby.BeanConverter;
import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.RegistryException;
import io.jooby.ServerOptions;
import io.jooby.ServiceKey;
import io.jooby.SessionStore;
import io.jooby.StatusCodeException;
import io.jooby.ErrorHandler;
import io.jooby.ExecutionMode;
import io.jooby.MediaType;
import io.jooby.MessageDecoder;
import io.jooby.MessageEncoder;
import io.jooby.ResponseHandler;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.RouterOptions;
import io.jooby.ServiceRegistry;
import io.jooby.StatusCode;
import io.jooby.TemplateEngine;
import io.jooby.WebSocket;
import io.jooby.internal.asm.ClassSource;
import io.jooby.ValueConverter;
import io.jooby.internal.handler.WebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

public class RouterImpl implements Router {

  private static class PathBuilder {
    private StringBuilder buffer;

    public PathBuilder(String... path) {
      Stream.of(path).forEach(this::append);
    }

    public PathBuilder append(String path) {
      if (!path.equals("/")) {
        if (buffer == null) {
          buffer = new StringBuilder();
        }
        buffer.append(path);
      }
      return this;
    }

    @Override public String toString() {
      return buffer == null ? "/" : buffer.toString();
    }
  }

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

    public boolean hasPattern() {
      return pattern != null;
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

  private static final Route ROUTE_MARK = new Route(Router.GET, "/", null);

  private ErrorHandler err;

  private Map<String, StatusCode> errorCodes;

  private RouterOptions options = new RouterOptions();

  private RouteTree chi = new Chi();

  private LinkedList<Stack> stack = new LinkedList<>();

  private List<Route> routes = new ArrayList<>();

  private HttpMessageEncoder encoder = new HttpMessageEncoder();

  private String basePath;

  private Map<Predicate<Context>, RouteTree> predicateMap;

  private Executor worker = new ForwardingExecutor();

  private Map<Route, Executor> routeExecutor = new HashMap<>();

  private Map<String, MessageDecoder> decoders = new HashMap<>();

  private Map<String, Object> attributes = new ConcurrentHashMap<>();

  private List<ResponseHandler> handlers = new ArrayList<>();

  private ServiceRegistry services = new ServiceRegistryImpl();

  private SessionStore sessionStore = SessionStore.memory();

  private String flashName = "jooby.flash";

  private List<ValueConverter> converters;

  private List<BeanConverter> beanConverters;

  private ClassLoader classLoader;

  public RouterImpl(ClassLoader loader) {
    this.classLoader = loader;
    stack.addLast(new Stack(null));

    converters = ValueConverters.defaultConverters();
    beanConverters = new ArrayList<>(3);
  }

  @Nonnull @Override public Config getConfig() {
    throw new UnsupportedOperationException();
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
    this.basePath = Router.path(basePath);
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
    Chi tree = new Chi();
    if (predicateMap == null) {
      predicateMap = new LinkedHashMap<>();
    }
    predicateMap.put(predicate, tree);
    for (Route route : router.getRoutes()) {
      Route newRoute = newRoute(route.getMethod(), route.getPattern(), route.getHandler(),
          tree);
      copy(route, newRoute);
    }
    return this;
  }

  @Nonnull @Override public Router use(@Nonnull String path, @Nonnull Router router) {
    String prefix = Router.path(path);
    for (Route route : router.getRoutes()) {
      String routePattern = new PathBuilder(prefix, route.getPattern()).toString();
      Route newRoute = newRoute(route.getMethod(), routePattern, route.getHandler(), chi);
      copy(route, newRoute);
    }
    return this;
  }

  @Nonnull @Override
  public Router use(@Nonnull Router router) {
    return use("/", router);
  }

  @Nonnull @Override public Router mvc(@Nonnull Object router) {
    throw new UnsupportedOperationException();
  }

  @Nonnull @Override public Router mvc(@Nonnull Class router) {
    throw new UnsupportedOperationException();
  }

  @Nonnull @Override
  public <T> Router mvc(@Nonnull Class<T> router, @Nonnull Provider<T> provider) {
    throw new UnsupportedOperationException();
  }

  @Nonnull @Override public Router encoder(@Nonnull MessageEncoder encoder) {
    this.encoder.add(encoder);
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
    decoders.put(contentType.getValue(), decoder);
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

  @Nonnull @Override
  public Router dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    return newStack(push().executor(executor), action);
  }

  @Nonnull @Override public Router route(@Nonnull Runnable action) {
    return newStack("/", action);
  }

  @Override @Nonnull public Router path(@Nonnull String pattern, @Nonnull Runnable action) {
    return newStack(pattern, action);
  }

  @Nonnull @Override public SessionStore getSessionStore() {
    return sessionStore;
  }

  @Nonnull @Override public Router setSessionStore(SessionStore sessionStore) {
    this.sessionStore = sessionStore;
    return this;
  }

  @Nonnull @Override public Router converter(ValueConverter converter) {
    converters.add(converter);
    return this;
  }

  @Nonnull @Override public Router converter(@Nonnull BeanConverter converter) {
    beanConverters.add(converter);
    return this;
  }

  @Nonnull @Override public List<ValueConverter> getConverters() {
    return converters;
  }

  @Nonnull @Override public List<BeanConverter> getBeanConverters() {
    return beanConverters;
  }

  @Nonnull @Override
  public Route ws(@Nonnull String pattern, @Nonnull WebSocket.Initializer handler) {
    return route(WS, pattern, new WebSocketHandler(handler)).setHandle(handler);
  }

  @Override
  public Route route(@Nonnull String method, @Nonnull String pattern,
      @Nonnull Route.Handler handler) {
    return newRoute(method, pattern, handler, chi);
  }

  private Route newRoute(@Nonnull String method, @Nonnull String pattern,
      @Nonnull Route.Handler handler, RouteTree tree) {
    /** Pattern: */
    PathBuilder pathBuilder = new PathBuilder();
    stack.stream().filter(Stack::hasPattern).forEach(it -> pathBuilder.append(it.pattern));
    pathBuilder.append(pattern);

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
    String safePattern = pathBuilder.toString();
    Route route = new Route(method, safePattern, handler);
    route.setPathKeys(Router.pathKeys(safePattern));
    route.setBefore(before);
    route.setAfter(after);
    route.setDecorator(decorator);
    route.setEncoder(encoder);
    route.setDecoders(decoders);

    decoratorList.forEach(it -> it.setRoute(route));
    handler.setRoute(route);

    Stack stack = this.stack.peekLast();
    if (stack.executor != null) {
      routeExecutor.put(route, stack.executor);
    }

    String finalPattern = basePath == null
        ? safePattern
        : new PathBuilder(basePath, safePattern).toString();

    if (route.getMethod().equals(WS)) {
      tree.insert(GET, finalPattern, route);
      route.setReturnType(Context.class);
    } else {
      tree.insert(route.getMethod(), finalPattern, route);

      if (route.isHttpOptions()) {
        tree.insert(Router.OPTIONS, finalPattern, route);
      } else if (route.isHttpTrace()) {
        tree.insert(Router.TRACE, finalPattern, route);
      } else if (route.isHttpHead() && route.getMethod().equals(GET)) {
        tree.insert(Router.HEAD, finalPattern, route);
      }
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
    encoder.add(MessageEncoder.TO_STRING);

    // Must be last, as fallback
    ValueConverters.addFallbackConverters(converters);
    ValueConverters.addFallbackBeanConverters(beanConverters);

    ClassSource source = new ClassSource(classLoader);
    RouteAnalyzer analyzer = new RouteAnalyzer(source, false);

    ExecutionMode mode = owner.getExecutionMode();
    for (Route route : routes) {
      String executorKey = route.getExecutorKey();
      Executor executor;
      if (executorKey == null) {
        executor = routeExecutor.get(route);
        if (executor instanceof ForwardingExecutor) {
          executor = ((ForwardingExecutor) executor).executor;
        }
      } else {
        if (executorKey.equals("worker")) {
          executor = (worker instanceof ForwardingExecutor)
              ? ((ForwardingExecutor) worker).executor
              : worker;
        } else {
          executor = executor(executorKey);
        }
      }
      /** Return type: */
      if (route.getReturnType() == null) {
        route.setReturnType(analyzer.returnType(route.getHandle()));
      }

      /** Default web socket values: */
      if (route.getHandler() instanceof WebSocketHandler) {
        if (route.getConsumes().isEmpty()) {
          // default type
          route.setConsumes(Collections.singletonList(MediaType.json));
        }
        if (route.getProduces().isEmpty()) {
          // default type
          route.setProduces(Collections.singletonList(MediaType.json));
        }
      } else {
        /** Consumes && Produces (only for HTTP routes (not web socket) */
        route.setBefore(
            prependMediaType(route.getConsumes(), route.getBefore(), Route.SUPPORT_MEDIA_TYPE));
        route.setBefore(prependMediaType(route.getProduces(), route.getBefore(), Route.ACCEPT));
      }
      /** Response handler: */
      Route.Handler pipeline = Pipeline
          .compute(source.getLoader(), route, mode, executor, handlers);
      route.setPipeline(pipeline);
      /** Final render */
      if (route.getHandler() instanceof WebSocketHandler) {
        route.setEncoder(encoder.toWebSocketEncoder());
      } else {
        route.setEncoder(encoder);
      }
    }
    /** router options: */
    if (options.getIgnoreCase() || options.getIgnoreTrailingSlash()) {
      chi = new RouteTreeWithOptions(chi, options.getIgnoreCase(),
          options.getIgnoreTrailingSlash());
    }

    // unwrap executor
    worker = ((ForwardingExecutor) worker).executor;
    this.stack.forEach(Stack::clear);
    this.stack = null;
    routeExecutor.clear();
    routeExecutor = null;
    source.destroy();
    return this;
  }

  private Route.Before prependMediaType(List<MediaType> contentTypes, Route.Before before,
      Route.Before prefix) {
    if (contentTypes.size() > 0) {
      return before == null ? prefix : prefix.then(before);
    }
    return before;
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
    if (this.predicateMap != null) {
      this.predicateMap.values().forEach(RouteTree::destroy);
      this.predicateMap.clear();
      this.predicateMap = null;
    }
  }

  @Nonnull @Override public ErrorHandler getErrorHandler() {
    return err;
  }

  @Nonnull @Override public Match match(@Nonnull Context ctx) {
    if (predicateMap != null) {
      for (Map.Entry<Predicate<Context>, RouteTree> e : predicateMap.entrySet()) {
        if (e.getKey().test(ctx)) {
          RouterMatch match = e.getValue().find(ctx.getMethod(), ctx.getRequestPath(), encoder);
          if (match.matches) {
            return match;
          }
        }
      }
    }
    return chi.find(ctx.getMethod(), ctx.getRequestPath(), encoder);
  }

  @Override public boolean match(@Nonnull String pattern, @Nonnull String path) {
    Chi chi = new Chi();
    chi.insert(Router.GET, pattern, ROUTE_MARK);
    return chi.find(Router.GET, path);
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

  @Nonnull @Override public ServerOptions getServerOptions() {
    throw new UnsupportedOperationException();
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
    Stack stack = new Stack(Router.path(pattern));
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
    it.setReturnType(src.getReturnType());
    it.setAttributes(src.getAttributes());
    it.setExecutorKey(src.getExecutorKey());
    it.setHandle(src.getHandle());
  }
}
