/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import jakarta.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import io.jooby.BeanConverter;
import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.Environment;
import io.jooby.ErrorHandler;
import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.MessageDecoder;
import io.jooby.MessageEncoder;
import io.jooby.ResponseHandler;
import io.jooby.Route;
import io.jooby.RouteSet;
import io.jooby.Router;
import io.jooby.RouterOption;
import io.jooby.ServerOptions;
import io.jooby.ServerSentEmitter;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.SessionStore;
import io.jooby.StatusCode;
import io.jooby.ValueConverter;
import io.jooby.WebSocket;
import io.jooby.exception.RegistryException;
import io.jooby.exception.StatusCodeException;
import io.jooby.internal.asm.ClassSource;
import io.jooby.internal.handler.ServerSentEventHandler;
import io.jooby.internal.handler.WebSocketHandler;

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
    private RouteTree tree;
    private String pattern;
    private Executor executor;
    private List<Route.Decorator> decoratorList = new ArrayList<>();
    private List<Route.Before> beforeList = new ArrayList<>();
    private List<Route.After> afterList = new ArrayList<>();

    public Stack(RouteTree tree, String pattern) {
      this.tree = tree;
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

  private Cookie flashCookie = new Cookie("jooby.flash").setHttpOnly(true);

  private List<ValueConverter> converters;

  private List<BeanConverter> beanConverters;

  private ClassLoader classLoader;

  private ContextInitializer preDispatchInitializer;

  private ContextInitializer postDispatchInitializer;

  private Set<RouterOption> routerOptions = EnumSet.of(RouterOption.RESET_HEADERS_ON_ERROR);

  private boolean trustProxy;

  private boolean contextAsService;

  public RouterImpl(ClassLoader loader) {
    this.classLoader = loader;
    stack.addLast(new Stack(chi, null));

    converters = ValueConverters.defaultConverters();
    beanConverters = new ArrayList<>(3);
  }

  @NonNull @Override public Config getConfig() {
    throw new UnsupportedOperationException();
  }

  @NonNull @Override public Environment getEnvironment() {
    throw new UnsupportedOperationException();
  }

  @NonNull @Override public List<Locale> getLocales() {
    throw new UnsupportedOperationException();
  }

  @NonNull @Override public Map<String, Object> getAttributes() {
    return attributes;
  }

  @NonNull @Override public Set<RouterOption> getRouterOptions() {
    return routerOptions;
  }

  @NonNull @Override public Router setRouterOptions(@NonNull RouterOption... options) {
    Stream.of(options).forEach(routerOptions::add);
    return this;
  }

  @NonNull @Override public Router setContextPath(@NonNull String basePath) {
    if (routes.size() > 0) {
      throw new IllegalStateException("Base path must be set before adding any routes.");
    }
    this.basePath = Router.leadingSlash(basePath);
    return this;
  }

  @NonNull @Override public Path getTmpdir() {
    return Paths.get(System.getProperty("java.io.tmpdir"));
  }

  @NonNull @Override public String getContextPath() {
    return basePath == null ? "/" : basePath;
  }

  @NonNull @Override public List<Route> getRoutes() {
    return routes;
  }

  @Override public boolean isTrustProxy() {
    return trustProxy;
  }

  @NonNull @Override public Router setTrustProxy(boolean trustProxy) {
    this.trustProxy = trustProxy;
    if (trustProxy) {
      addPreDispatchInitializer(ContextInitializer.PROXY_PEER_ADDRESS);
    } else {
      removePreDispatchInitializer(ContextInitializer.PROXY_PEER_ADDRESS);
    }
    return this;
  }

  @NonNull @Override public RouteSet domain(@NonNull String domain, @NonNull Runnable body) {
    return use(domainPredicate(domain), body);
  }

  @NonNull @Override public Router domain(@NonNull String domain, @NonNull Router subrouter) {
    return use(domainPredicate(domain), subrouter);
  }

  @NonNull @Override
  public RouteSet mount(@NonNull Predicate<Context> predicate, @NonNull Runnable body) {
    RouteSet routeSet = new RouteSet();
    Chi tree = new Chi();
    putPredicate(predicate, tree);
    int start = this.routes.size();
    newStack(tree, "/", body);
    routeSet.setRoutes(this.routes.subList(start, this.routes.size()));
    return routeSet;
  }

  @NonNull @Override
  public Router mount(@NonNull Predicate<Context> predicate, @NonNull Router subrouter) {
    /** Override services: */
    overrideServices(subrouter);

    /** Routes: */
    mount(predicate, () -> {
      for (Route route : subrouter.getRoutes()) {
        Route newRoute = newRoute(route.getMethod(), route.getPattern(), route.getHandler());
        copy(route, newRoute);
      }
    });
    return this;
  }

  @NonNull @Override public Router mount(@NonNull String path, @NonNull Router router) {
    /** Override services: */
    overrideServices(router);
    /** Merge error handler:  */
    mergeErrorHandler(router);
    /** Routes: */
    copyRoutes(path, router);
    return this;
  }

  @NonNull @Override
  public Router mount(@NonNull Router router) {
    return mount("/", router);
  }

  @NonNull @Override public Router mvc(@NonNull Object router) {
    throw new UnsupportedOperationException();
  }

  @NonNull @Override public Router mvc(@NonNull Class router) {
    throw new UnsupportedOperationException();
  }

  @NonNull @Override
  public <T> Router mvc(@NonNull Class<T> router, @NonNull Provider<T> provider) {
    throw new UnsupportedOperationException();
  }

  @NonNull @Override public Router encoder(@NonNull MessageEncoder encoder) {
    this.encoder.add(MediaType.all, encoder);
    return this;
  }

  @NonNull @Override
  public Router encoder(@NonNull MediaType contentType, @NonNull MessageEncoder encoder) {
    this.encoder.add(contentType, encoder);
    return this;
  }

  @NonNull @Override public Router decoder(@NonNull MediaType contentType, @NonNull
      MessageDecoder decoder) {
    decoders.put(contentType.getValue(), decoder);
    return this;
  }

  @NonNull @Override public Executor getWorker() {
    return worker;
  }

  @NonNull @Override public Router setWorker(Executor worker) {
    ForwardingExecutor workerRef = (ForwardingExecutor) this.worker;
    workerRef.executor = worker;
    return this;
  }

  @NonNull @Override public Router setDefaultWorker(@NonNull Executor worker) {
    ForwardingExecutor workerRef = (ForwardingExecutor) this.worker;
    if (workerRef.executor == null) {
      workerRef.executor = worker;
    }
    return this;
  }

  @Override @NonNull public Router decorator(@NonNull Route.Decorator decorator) {
    stack.peekLast().then(decorator);
    return this;
  }

  @Override @NonNull public Router after(@NonNull Route.After after) {
    stack.peekLast().then(after);
    return this;
  }

  @NonNull @Override public Router before(@NonNull Route.Before before) {
    stack.peekLast().then(before);
    return this;
  }

  @NonNull @Override public Router error(@NonNull ErrorHandler handler) {
    err = err == null ? handler : err.then(handler);
    return this;
  }

  @NonNull @Override public Router dispatch(@NonNull Runnable body) {
    return newStack(push(chi).executor(worker), body);
  }

  @NonNull @Override
  public Router dispatch(@NonNull Executor executor, @NonNull Runnable action) {
    return newStack(push(chi).executor(executor), action);
  }

  @NonNull @Override public RouteSet routes(@NonNull Runnable action) {
    return path("/", action);
  }

  @Override @NonNull public RouteSet path(@NonNull String pattern, @NonNull Runnable action) {
    RouteSet routeSet = new RouteSet();
    int start = this.routes.size();
    newStack(chi, pattern, action);
    routeSet.setRoutes(this.routes.subList(start, this.routes.size()));
    return routeSet;
  }

  @NonNull @Override public SessionStore getSessionStore() {
    return sessionStore;
  }

  @NonNull @Override public Router setSessionStore(SessionStore sessionStore) {
    this.sessionStore = sessionStore;
    return this;
  }

  @NonNull @Override public Router converter(ValueConverter converter) {
    converters.add(converter);
    return this;
  }

  @NonNull @Override public Router converter(@NonNull BeanConverter converter) {
    beanConverters.add(converter);
    return this;
  }

  @NonNull @Override public List<ValueConverter> getConverters() {
    return converters;
  }

  @NonNull @Override public List<BeanConverter> getBeanConverters() {
    return beanConverters;
  }

  @NonNull @Override
  public Route ws(@NonNull String pattern, @NonNull WebSocket.Initializer handler) {
    return route(WS, pattern, new WebSocketHandler(handler)).setHandle(handler);
  }

  @NonNull @Override
  public Route sse(@NonNull String pattern, @NonNull ServerSentEmitter.Handler handler) {
    return route(SSE, pattern, new ServerSentEventHandler(handler)).setHandle(handler)
        .setExecutorKey("worker");
  }

  @Override
  public Route route(@NonNull String method, @NonNull String pattern,
      @NonNull Route.Handler handler) {
    return newRoute(method, pattern, handler);
  }

  private Route newRoute(@NonNull String method, @NonNull String pattern,
      @NonNull Route.Handler handler) {
    RouteTree tree = stack.getLast().tree;
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

    if (routerOptions.contains(RouterOption.IGNORE_CASE)) {
      finalPattern = finalPattern.toLowerCase();
    }

    for (String routePattern : Router.expandOptionalVariables(finalPattern)) {
      if (route.getMethod().equals(WS)) {
        tree.insert(GET, routePattern, route);
        route.setReturnType(Context.class);
      } else if (route.getMethod().equals(SSE)) {
        tree.insert(GET, routePattern, route);
        route.setReturnType(Context.class);
      } else {
        tree.insert(route.getMethod(), routePattern, route);

        if (route.isHttpOptions()) {
          tree.insert(Router.OPTIONS, routePattern, route);
        } else if (route.isHttpTrace()) {
          tree.insert(Router.TRACE, routePattern, route);
        } else if (route.isHttpHead() && route.getMethod().equals(GET)) {
          tree.insert(Router.HEAD, routePattern, route);
        }
      }
    }
    routes.add(route);

    return route;
  }

  @NonNull public Router start(@NonNull Jooby app) {
    if (err == null) {
      err = ErrorHandler.create();
    } else {
      err = err.then(ErrorHandler.create());
    }

    // Must be last, as fallback
    ValueConverter.addFallbackConverters(converters);
    BeanConverter.addFallbackConverters(beanConverters);

    ClassSource source = new ClassSource(classLoader);
    RouteAnalyzer analyzer = new RouteAnalyzer(source, false);

    ExecutionMode mode = app.getExecutionMode();
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
          .compute(source.getLoader(), route, forceMode(route, mode), executor,
              postDispatchInitializer, handlers);
      route.setPipeline(pipeline);
      /** Final render */
      route.setEncoder(encoder);
    }
    ((Chi) chi).setEncoder(encoder);

    /** router options: */
    if (routerOptions.contains(RouterOption.IGNORE_CASE)) {
      chi = new RouteTreeLowerCasePath(chi);
    }
    if (routerOptions.contains(RouterOption.IGNORE_TRAILING_SLASH)) {
      chi = new RouteTreeIgnoreTrailingSlash(chi);
    }
    if (routerOptions.contains(RouterOption.NORMALIZE_SLASH)) {
      chi = new RouteTreeNormPath(chi);
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

  private ExecutionMode forceMode(Route route, ExecutionMode mode) {
    if (route.getMethod().equals(Router.WS)) {
      // websocket always run in worker executor
      return ExecutionMode.WORKER;
    }
    return mode;
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

  @NonNull @Override public Router executor(@NonNull String name, @NonNull Executor executor) {
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

  @NonNull @Override public ErrorHandler getErrorHandler() {
    return err;
  }

  @NonNull @Override public Match match(@NonNull Context ctx) {
    if (preDispatchInitializer != null) {
      preDispatchInitializer.apply(ctx);
    }
    if (predicateMap != null) {
      for (Map.Entry<Predicate<Context>, RouteTree> e : predicateMap.entrySet()) {
        if (e.getKey().test(ctx)) {
          Router.Match match = e.getValue().find(ctx.getMethod(), ctx.getRequestPath());
          if (match.matches()) {
            return match;
          }
        }
      }
    }
    return chi.find(ctx.getMethod(), ctx.getRequestPath());
  }

  @Override public boolean match(@NonNull String pattern, @NonNull String path) {
    Chi chi = new Chi();
    chi.insert(Router.GET, pattern, ROUTE_MARK);
    return chi.exists(Router.GET, path);
  }

  @NonNull @Override public Router errorCode(@NonNull Class<? extends Throwable> type,
      @NonNull StatusCode statusCode) {
    if (errorCodes == null) {
      errorCodes = new HashMap<>();
    }
    this.errorCodes.put(type.getCanonicalName(), statusCode);
    return this;
  }

  @NonNull @Override public StatusCode errorCode(@NonNull Throwable x) {
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
    if (x instanceof IllegalArgumentException || x instanceof NoSuchElementException) {
      return StatusCode.BAD_REQUEST;
    }
    if (x instanceof FileNotFoundException) {
      return StatusCode.NOT_FOUND;
    }
    return StatusCode.SERVER_ERROR;
  }

  @NonNull @Override public Router responseHandler(ResponseHandler handler) {
    handlers.add(handler);
    return this;
  }

  @NonNull @Override public ServiceRegistry getServices() {
    return services;
  }

  @NonNull @Override public <T> T require(@NonNull Class<T> type, @NonNull String name)
      throws RegistryException {
    return services.require(type, name);
  }

  @NonNull @Override public <T> T require(@NonNull Class<T> type) throws RegistryException {
    return services.require(type);
  }

  @NonNull @Override public <T> T require(@NonNull ServiceKey<T> key) throws RegistryException {
    return services.require(key);
  }

  @NonNull @Override public Router setFlashCookie(@NonNull String name) {
    this.flashCookie.setName(name);
    return this;
  }

  @NonNull @Override public Cookie getFlashCookie() {
    return flashCookie;
  }

  @NonNull @Override public Router setFlashCookie(@NonNull Cookie flashCookie) {
    this.flashCookie = requireNonNull(flashCookie);
    return this;
  }

  @NonNull @Override public ServerOptions getServerOptions() {
    throw new UnsupportedOperationException();
  }

  @NonNull @Override public Router setHiddenMethod(@NonNull String parameterName) {
    setHiddenMethod(new DefaultHiddenMethodLookup(parameterName));
    return this;
  }

  @NonNull @Override public Router setHiddenMethod(
      @NonNull Function<Context, Optional<String>> provider) {
    addPreDispatchInitializer(new HiddenMethodInitializer(provider));
    return this;
  }

  @NonNull @Override public Router setCurrentUser(
      @NonNull Function<Context, Object> provider) {
    addPreDispatchInitializer(new CurrentUserInitializer(provider));
    return this;
  }

  @NonNull @Override public Router setContextAsService(boolean contextAsService) {
    if (this.contextAsService == contextAsService) {
      return this;
    }

    this.contextAsService = contextAsService;

    if (contextAsService) {
      addPostDispatchInitializer(ContextAsServiceInitializer.INSTANCE);
      getServices().put(Context.class, ContextAsServiceInitializer.INSTANCE);
    } else {
      removePostDispatchInitializer(ContextAsServiceInitializer.INSTANCE);
      getServices().put(Context.class, (Provider<Context>) null);
    }

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

  private Router newStack(RouteTree tree, String pattern, Runnable action,
      Route.Decorator... decorator) {
    return newStack(push(tree, pattern), action, decorator);
  }

  private Stack push(RouteTree tree) {
    return new Stack(tree, null);
  }

  private Stack push(RouteTree tree, String pattern) {
    Stack stack = new Stack(tree, Router.leadingSlash(pattern));
    if (this.stack.size() > 0) {
      Stack parent = this.stack.getLast();
      stack.executor = parent.executor;
    }
    return stack;
  }

  private Router newStack(@NonNull Stack stack, @NonNull Runnable action,
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

  private void putPredicate(@NonNull Predicate<Context> predicate, Chi tree) {
    if (predicateMap == null) {
      predicateMap = new LinkedHashMap<>();
    }
    predicateMap.put(predicate, tree);
  }

  private void removePreDispatchInitializer(ContextInitializer initializer) {
    if (this.preDispatchInitializer instanceof ContextInitializerList) {
      ((ContextInitializerList) initializer).remove(initializer);
    } else if (this.preDispatchInitializer == initializer) {
      this.preDispatchInitializer = null;
    }
  }

  private void addPreDispatchInitializer(ContextInitializer initializer) {
    if (this.preDispatchInitializer instanceof ContextInitializerList) {
      this.preDispatchInitializer.add(initializer);
    } else if (this.preDispatchInitializer != null) {
      this.preDispatchInitializer = new ContextInitializerList(this.preDispatchInitializer)
          .add(initializer);
    } else {
      this.preDispatchInitializer = initializer;
    }
  }

  private void removePostDispatchInitializer(ContextInitializer initializer) {
    if (this.postDispatchInitializer instanceof ContextInitializerList) {
      ((ContextInitializerList) postDispatchInitializer).remove(initializer);
    } else if (this.postDispatchInitializer == initializer) {
      this.postDispatchInitializer = null;
    }
  }

  private void addPostDispatchInitializer(ContextInitializer initializer) {
    if (this.postDispatchInitializer instanceof ContextInitializerList) {
      this.postDispatchInitializer.add(initializer);
    } else if (this.postDispatchInitializer != null) {
      this.postDispatchInitializer = new ContextInitializerList(this.postDispatchInitializer)
          .add(initializer);
    } else {
      this.postDispatchInitializer = initializer;
    }
  }

  private static Predicate<Context> domainPredicate(String domain) {
    return ctx -> ctx.getHost().equals(domain);
  }

  private void copyRoutes(@NonNull String path, @NonNull Router router) {
    String prefix = Router.leadingSlash(path);
    for (Route route : router.getRoutes()) {
      String routePattern = new PathBuilder(prefix, route.getPattern()).toString();
      Route newRoute = newRoute(route.getMethod(), routePattern, route.getHandler());
      copy(route, newRoute);
    }
  }

  private void overrideServices(Router router) {
    if (router instanceof Jooby) {
      Jooby app = (Jooby) router;
      overrideServices(app.getRouter());
    } else if (router instanceof RouterImpl) {
      RouterImpl that = (RouterImpl) router;
      // Inherited the services from router owner
      that.services = this.services;
    }
  }

  private void mergeErrorHandler(Router router) {
    if (router instanceof Jooby) {
      Jooby app = (Jooby) router;
      mergeErrorHandler(app.getRouter());
    } else if (router instanceof RouterImpl) {
      RouterImpl that = (RouterImpl) router;
      if (that.err != null) {
        this.error(that.err);
      }
    }
  }
}
