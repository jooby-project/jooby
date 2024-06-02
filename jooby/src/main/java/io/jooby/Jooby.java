/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.buffer.DataBufferFactory;
import io.jooby.exception.RegistryException;
import io.jooby.exception.StartupException;
import io.jooby.internal.LocaleUtils;
import io.jooby.internal.MutedServer;
import io.jooby.internal.RegistryRef;
import io.jooby.internal.RouterImpl;
import jakarta.inject.Provider;

/**
 * Welcome to Jooby!
 *
 * <p>Hello World:
 *
 * <pre>{@code
 * public class App extends Jooby {
 *
 *   {
 *     get("/", ctx -> "Hello World!");
 *   }
 *
 *   public static void main(String[] args) {
 *     runApp(args, App::new);
 *   }
 * }
 *
 * }</pre>
 *
 * More documentation at <a href="https://jooby.io">jooby.io</a>
 *
 * @author edgar
 * @since 2.0.0
 */
public class Jooby implements Router, Registry {

  static final String APP_NAME = "___app_name__";

  private static final String JOOBY_RUN_HOOK = "___jooby_run_hook__";

  private final transient AtomicBoolean started = new AtomicBoolean(true);

  private final transient AtomicBoolean stopped = new AtomicBoolean(false);

  private static Jooby owner;

  private RouterImpl router;

  private ExecutionMode mode;

  private Path tmpdir;

  private List<SneakyThrows.Runnable> readyCallbacks;

  private List<SneakyThrows.Runnable> startingCallbacks;

  private LinkedList<AutoCloseable> stopCallbacks;

  private List<Extension> lateExtensions;

  private Environment env;

  private RegistryRef registry = new RegistryRef();

  private ServerOptions serverOptions;

  private List<StartupSummary> startupSummary;

  private EnvironmentOptions environmentOptions;

  private List<Locale> locales;

  private boolean lateInit;

  private String name;

  private String basePackage;

  private String version;

  private Server server;

  /** Creates a new Jooby instance. */
  public Jooby() {
    if (owner == null) {
      ClassLoader classLoader = getClass().getClassLoader();
      environmentOptions = new EnvironmentOptions().setClassLoader(classLoader);
      router = new RouterImpl();
      stopCallbacks = new LinkedList<>();
      startingCallbacks = new ArrayList<>();
      readyCallbacks = new ArrayList<>();
      lateExtensions = new ArrayList<>();
    } else {
      copyState(owner, this);
    }
  }

  /**
   * Server options or <code>null</code>.
   *
   * @return Server options or <code>null</code>.
   */
  public @Nullable ServerOptions getServerOptions() {
    return serverOptions;
  }

  /**
   * Set server options.
   *
   * @param serverOptions Server options.
   * @return This application.
   */
  public @NonNull Jooby setServerOptions(@NonNull ServerOptions serverOptions) {
    this.serverOptions = serverOptions;
    return this;
  }

  @NonNull @Override
  public Set<RouterOption> getRouterOptions() {
    return router.getRouterOptions();
  }

  @NonNull @Override
  public Jooby setRouterOptions(@NonNull RouterOption... options) {
    router.setRouterOptions(options);
    return this;
  }

  /**
   * Application environment. If none was set, environment is initialized using {@link
   * Environment#loadEnvironment(EnvironmentOptions)}.
   *
   * @return Application environment.
   */
  public @NonNull Environment getEnvironment() {
    if (env == null) {
      env = Environment.loadEnvironment(environmentOptions);
    }
    return env;
  }

  /**
   * Returns the list of supported locales, or {@code null} if none set.
   *
   * @return The supported locales.
   */
  @Nullable @Override
  public List<Locale> getLocales() {
    return locales;
  }

  /**
   * Sets the supported locales.
   *
   * @param locales The supported locales.
   * @return This router.
   */
  public Router setLocales(@NonNull List<Locale> locales) {
    this.locales = requireNonNull(locales);
    return this;
  }

  /**
   * Sets the supported locales.
   *
   * @param locales The supported locales.
   * @return This router.
   */
  public Router setLocales(Locale... locales) {
    return setLocales(Arrays.asList(locales));
  }

  /**
   * Application class loader.
   *
   * @return Application class loader.
   */
  public @NonNull ClassLoader getClassLoader() {
    return env == null ? environmentOptions.getClassLoader() : env.getClassLoader();
  }

  /**
   * Application configuration. It is a shortcut for {@link Environment#getConfig()}.
   *
   * @return Application config.
   */
  public @NonNull Config getConfig() {
    return getEnvironment().getConfig();
  }

  /**
   * Set application environment.
   *
   * @param environment Application environment.
   * @return This application.
   */
  public @NonNull Jooby setEnvironment(@NonNull Environment environment) {
    this.env = environment;
    return this;
  }

  /**
   * Set environment options and initialize/overrides the environment.
   *
   * @param options Environment options.
   * @return New environment.
   */
  public @NonNull Environment setEnvironmentOptions(@NonNull EnvironmentOptions options) {
    this.environmentOptions = options;
    this.env =
        Environment.loadEnvironment(
            options.setClassLoader(options.getClassLoader(getClass().getClassLoader())));
    return this.env;
  }

  /**
   * Event fired before starting router and web-server. Non-lateinit extension are installed at this
   * stage.
   *
   * @param body Start body.
   * @return This application.
   */
  public @NonNull Jooby onStarting(@NonNull SneakyThrows.Runnable body) {
    startingCallbacks.add(body);
    return this;
  }

  /**
   * Event is fire once all components has been initialized, for example router and web-server are
   * up and running, extension installed, etc...
   *
   * @param body Start body.
   * @return This application.
   */
  public @NonNull Jooby onStarted(@NonNull SneakyThrows.Runnable body) {
    readyCallbacks.add(body);
    return this;
  }

  /**
   * Stop event is fire at application shutdown time. Useful to execute cleanup task, free
   * resources, etc...
   *
   * @param body Stop body.
   * @return This application.
   */
  public @NonNull Jooby onStop(@NonNull AutoCloseable body) {
    stopCallbacks.addFirst(body);
    return this;
  }

  @NonNull @Override
  public Jooby setContextPath(@NonNull String basePath) {
    router.setContextPath(basePath);
    return this;
  }

  @NonNull @Override
  public String getContextPath() {
    return router.getContextPath();
  }

  /**
   * Installs/imports a full application into this one. Applications share services, registry,
   * callbacks, etc.
   *
   * <p>Applications must be instantiated/created lazily via a supplier/factory. This is required
   * due to the way an application is usually initialized (constructor initializer).
   *
   * <p>Working example:
   *
   * <pre>{@code
   * install(SubApp::new);
   *
   * }</pre>
   *
   * Lazy creation configures and setup <code>SubApp</code> correctly, the next example won't work:
   *
   * <pre>{@code
   * SubApp app = new SubApp();
   * install(app); // WONT WORK
   *
   * }</pre>
   *
   * Note: you must take care of application services across the applications. For example make sure
   * you don't configure the same service twice or more in the main and imported applications too.
   *
   * @param factory Application factory.
   * @return This application.
   */
  @NonNull public Jooby install(@NonNull SneakyThrows.Supplier<Jooby> factory) {
    return install("/", factory);
  }

  /**
   * Installs/imports a full application into this one. Applications share services, registry,
   * callbacks, etc.
   *
   * <p>Application must be instantiated/created lazily via a supplier/factory. This is required due
   * to the way an application is usually initialized (constructor initializer).
   *
   * <p>Working example:
   *
   * <pre>{@code
   * install("/subapp", SubApp::new);
   *
   * }</pre>
   *
   * Lazy creation allows to configure and setup <code>SubApp</code> correctly, the next example
   * won't work:
   *
   * <pre>{@code
   * SubApp app = new SubApp();
   * install("/subapp", app); // WONT WORK
   *
   * }</pre>
   *
   * Note: you must take care of application services across the applications. For example make sure
   * you don't configure the same service twice or more in the main and imported applications too.
   *
   * @param path Path prefix.
   * @param factory Application factory.
   * @return This application.
   */
  @NonNull public Jooby install(@NonNull String path, @NonNull SneakyThrows.Supplier<Jooby> factory) {
    try {
      owner = this;
      path(path, factory::get);
      return this;
    } finally {
      owner = null;
    }
  }

  /**
   * Installs/imports a full application into this one. Applications share services, registry,
   * callbacks, etc.
   *
   * <p>Application must be instantiated/created lazily via a supplier/factory. This is required due
   * to the way an application is usually initialized (constructor initializer).
   *
   * <p>Working example:
   *
   * <pre>{@code
   * install("/subapp", ctx -> ctx.header("v").value("").equals("1.0"), SubApp::new);
   *
   * }</pre>
   *
   * Lazy creation allows to configure and setup <code>SubApp</code> correctly, the next example
   * won't work:
   *
   * <pre>{@code
   * SubApp app = new SubApp();
   * install("/subapp", ctx -> ctx.header("v").value("").equals("1.0"), app); // WONT WORK
   *
   * }</pre>
   *
   * Note: you must take care of application services across the applications. For example make sure
   * you don't configure the same service twice or more in the main and imported applications too.
   *
   * @param path Sub path.
   * @param predicate HTTP predicate.
   * @param factory Application factory.
   * @return This application.
   */
  @NonNull public Jooby install(
      @NonNull String path,
      @NonNull Predicate<Context> predicate,
      @NonNull SneakyThrows.Supplier<Jooby> factory) {
    try {
      owner = this;
      router.install(path, predicate, factory);
      return this;
    } finally {
      owner = null;
    }
  }

  /**
   * Installs/imports a full application into this one. Applications share services, registry,
   * callbacks, etc.
   *
   * <p>Application must be instantiated/created lazily via a supplier/factory. This is required due
   * to the way an application is usually initialized (constructor initializer).
   *
   * <p>Working example:
   *
   * <pre>{@code
   * install(ctx -> ctx.header("v").value("").equals("1.0"), SubApp::new);
   *
   * }</pre>
   *
   * Lazy creation allows to configure and setup <code>SubApp</code> correctly, the next example
   * won't work:
   *
   * <pre>{@code
   * SubApp app = new SubApp();
   * install(ctx -> ctx.header("v").value("").equals("1.0"), app); // WONT WORK
   *
   * }</pre>
   *
   * Note: you must take care of application services across the applications. For example make sure
   * you don't configure the same service twice or more in the main and imported applications too.
   *
   * @param predicate HTTP predicate.
   * @param factory Application factory.
   * @return This application.
   */
  @NonNull public Jooby install(
      @NonNull Predicate<Context> predicate, @NonNull SneakyThrows.Supplier<Jooby> factory) {
    return install("/", predicate, factory);
  }

  /**
   * The underlying router.
   *
   * @return The underlying router.
   */
  public @NonNull Router getRouter() {
    return router;
  }

  @Override
  public boolean isTrustProxy() {
    return router.isTrustProxy();
  }

  @Override
  public boolean isStarted() {
    return started.get();
  }

  @Override
  public boolean isStopped() {
    return stopped.get();
  }

  @NonNull @Override
  public Jooby setTrustProxy(boolean trustProxy) {
    this.router.setTrustProxy(trustProxy);
    return this;
  }

  @NonNull @Override
  public Router domain(@NonNull String domain, @NonNull Router subrouter) {
    this.router.domain(domain, subrouter);
    return this;
  }

  @NonNull @Override
  public RouteSet domain(@NonNull String domain, @NonNull Runnable body) {
    return router.domain(domain, body);
  }

  @NonNull @Override
  public RouteSet mount(@NonNull Predicate<Context> predicate, @NonNull Runnable body) {
    return router.mount(predicate, body);
  }

  @NonNull @Override
  public Jooby mount(@NonNull Predicate<Context> predicate, @NonNull Router subrouter) {
    this.router.mount(predicate, subrouter);
    return this;
  }

  @NonNull @Override
  public Jooby mount(@NonNull String path, @NonNull Router router) {
    this.router.mount(path, router);
    if (router instanceof Jooby) {
      Jooby child = (Jooby) router;
      child.registry = this.registry;
    }
    return this;
  }

  @NonNull @Override
  public Jooby mount(@NonNull Router router) {
    return mount("/", router);
  }

  @NonNull @Override
  public Jooby mvc(@NonNull MvcExtension router) {
    try {
      router.install(this);
      return this;
    } catch (Exception cause) {
      throw SneakyThrows.propagate(cause);
    }
  }

  @NonNull @Override
  public Jooby mvc(@NonNull Object router) {
    Provider provider = () -> router;
    return mvc(router.getClass(), provider);
  }

  @NonNull @Override
  public Jooby mvc(@NonNull Class router) {
    return mvc(router, () -> require(router));
  }

  @NonNull @Override
  public <T> Jooby mvc(@NonNull Class<T> router, @NonNull Provider<T> provider) {
    try {
      MvcFactory module = loadModule(router);
      Extension extension = module.create(provider);
      extension.install(this);
      return this;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private MvcFactory loadModule(Class router) {
    try {
      ServiceLoader<MvcFactory> modules = ServiceLoader.load(MvcFactory.class);
      return stream(modules.spliterator(), false)
          .filter(it -> it.supports(router))
          .findFirst()
          .orElseGet(
              () ->
                  /** Make happy IDE incremental build: */
                  mvcReflectionFallback(router, getClassLoader()));
    } catch (ServiceConfigurationError notfound) {
      /** Make happy IDE incremental build: */
      return mvcReflectionFallback(router, getClassLoader());
    }
  }

  @NonNull @Override
  public Route ws(@NonNull String pattern, @NonNull WebSocket.Initializer handler) {
    return router.ws(pattern, handler);
  }

  @NonNull @Override
  public Route sse(@NonNull String pattern, @NonNull ServerSentEmitter.Handler handler) {
    return router.sse(pattern, handler);
  }

  @NonNull @Override
  public List<Route> getRoutes() {
    return router.getRoutes();
  }

  @NonNull @Override
  public Jooby error(@NonNull ErrorHandler handler) {
    router.error(handler);
    return this;
  }

  @NonNull @Override
  public Router use(@NonNull Route.Filter filter) {
    router.use(filter);
    return this;
  }

  @NonNull @Override
  public Jooby before(@NonNull Route.Before before) {
    router.before(before);
    return this;
  }

  @NonNull @Override
  public Jooby after(@NonNull Route.After after) {
    router.after(after);
    return this;
  }

  @NonNull @Override
  public Jooby encoder(@NonNull MessageEncoder encoder) {
    router.encoder(encoder);
    return this;
  }

  @NonNull @Override
  public Jooby decoder(@NonNull MediaType contentType, @NonNull MessageDecoder decoder) {
    router.decoder(contentType, decoder);
    return this;
  }

  @NonNull @Override
  public Jooby encoder(@NonNull MediaType contentType, @NonNull MessageEncoder encoder) {
    router.encoder(contentType, encoder);
    return this;
  }

  /**
   * Install extension module.
   *
   * @param extension Extension module.
   * @return This application.
   */
  @NonNull public Jooby install(@NonNull Extension extension) {
    if (lateInit || extension.lateinit()) {
      lateExtensions.add(extension);
    } else {
      try {
        extension.install(this);
      } catch (Exception x) {
        throw SneakyThrows.propagate(x);
      }
    }
    return this;
  }

  /**
   * Set server to use.
   *
   * @param server Web Server.
   * @return This application.
   */
  @NonNull public Jooby install(@NonNull Server server) {
    this.server = server;
    return this;
  }

  @NonNull @Override
  public Jooby dispatch(@NonNull Runnable body) {
    router.dispatch(body);
    return this;
  }

  @NonNull @Override
  public Jooby dispatch(@NonNull Executor executor, @NonNull Runnable action) {
    router.dispatch(executor, action);
    return this;
  }

  @NonNull @Override
  public RouteSet path(@NonNull String pattern, @NonNull Runnable action) {
    return router.path(pattern, action);
  }

  @NonNull @Override
  public RouteSet routes(@NonNull Runnable action) {
    return router.routes(action);
  }

  @NonNull @Override
  public Route route(
      @NonNull String method, @NonNull String pattern, @NonNull Route.Handler handler) {
    return router.route(method, pattern, handler);
  }

  @NonNull @Override
  public Match match(@NonNull Context ctx) {
    return router.match(ctx);
  }

  @Override
  public boolean match(@NonNull String pattern, @NonNull String path) {
    return router.match(pattern, path);
  }

  @NonNull @Override
  public Jooby errorCode(@NonNull Class<? extends Throwable> type, @NonNull StatusCode statusCode) {
    router.errorCode(type, statusCode);
    return this;
  }

  @NonNull @Override
  public StatusCode errorCode(@NonNull Throwable cause) {
    return router.errorCode(cause);
  }

  @NonNull @Override
  public Executor getWorker() {
    return router.getWorker();
  }

  @NonNull @Override
  public Jooby setWorker(@NonNull Executor worker) {
    this.router.setWorker(worker);
    if (worker instanceof ExecutorService) {
      onStop(((ExecutorService) worker)::shutdown);
    }
    return this;
  }

  @NonNull @Override
  public Jooby setDefaultWorker(@NonNull Executor worker) {
    this.router.setDefaultWorker(worker);
    return this;
  }

  @NonNull @Override
  public DataBufferFactory getBufferFactory() {
    return router.getBufferFactory();
  }

  @NonNull @Override
  public Jooby setBufferFactory(@NonNull DataBufferFactory bufferFactory) {
    router.setBufferFactory(bufferFactory);
    return this;
  }

  @NonNull @Override
  public Logger getLog() {
    return LoggerFactory.getLogger(getClass());
  }

  @NonNull @Override
  public Jooby resultHandler(ResultHandler handler) {
    router.resultHandler(handler);
    return this;
  }

  @NonNull @Override
  public ErrorHandler getErrorHandler() {
    return router.getErrorHandler();
  }

  @NonNull @Override
  public Path getTmpdir() {
    if (tmpdir == null) {
      tmpdir =
          Paths.get(getEnvironment().getConfig().getString(AvailableSettings.TMP_DIR))
              .toAbsolutePath();
    }
    return tmpdir;
  }

  /**
   * Set application temporary directory.
   *
   * @param tmpdir Temp directory.
   * @return This application.
   */
  public @NonNull Jooby setTmpdir(@NonNull Path tmpdir) {
    this.tmpdir = tmpdir;
    return this;
  }

  /**
   * Application execution mode.
   *
   * @return Application execution mode.
   */
  public @NonNull ExecutionMode getExecutionMode() {
    return mode == null ? ExecutionMode.DEFAULT : mode;
  }

  /**
   * Set application execution mode.
   *
   * @param mode Application execution mode.
   * @return This application.
   */
  public @NonNull Jooby setExecutionMode(@NonNull ExecutionMode mode) {
    this.mode = mode;
    return this;
  }

  @NonNull @Override
  public Map<String, Object> getAttributes() {
    return router.getAttributes();
  }

  @NonNull @Override
  public Jooby attribute(@NonNull String key, @NonNull Object value) {
    router.attribute(key, value);
    return this;
  }

  @NonNull @Override
  public <T> T attribute(@NonNull String key) {
    return router.attribute(key);
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type, @NonNull String name) {
    return require(ServiceKey.key(type, name));
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type) {
    return require(ServiceKey.key(type));
  }

  @Override
  public @NonNull <T> T require(@NonNull ServiceKey<T> key) {
    ServiceRegistry services = getServices();
    T service = services.getOrNull(key);
    if (service == null) {
      if (!registry.isSet()) {
        throw new RegistryException("Service not found: " + key);
      }
      String name = key.getName();
      return name == null
          ? registry.get().require(key.getType())
          : registry.get().require(key.getType(), name);
    }
    return service;
  }

  /**
   * Set application registry.
   *
   * @param registry Application registry.
   * @return This application.
   */
  @NonNull public Jooby registry(@NonNull Registry registry) {
    this.registry.set(registry);
    return this;
  }

  @NonNull @Override
  public ServiceRegistry getServices() {
    return this.router.getServices();
  }

  /**
   * Get base application package. This is the package from where application was initialized or the
   * package of a Jooby application sub-class.
   *
   * @return Base application package.
   */
  public @Nullable String getBasePackage() {
    if (basePackage == null) {
      basePackage =
          System.getProperty(
              AvailableSettings.PACKAGE,
              Optional.ofNullable(getClass().getPackage()).map(Package::getName).orElse(null));
    }
    return basePackage;
  }

  public @NonNull Jooby setBasePackage(@Nullable String basePackage) {
    this.basePackage = basePackage;
    return this;
  }

  @NonNull @Override
  public SessionStore getSessionStore() {
    return router.getSessionStore();
  }

  @NonNull @Override
  public Jooby setSessionStore(@NonNull SessionStore store) {
    router.setSessionStore(store);
    return this;
  }

  @NonNull @Override
  public Jooby executor(@NonNull String name, @NonNull Executor executor) {
    if (executor instanceof ExecutorService) {
      onStop(((ExecutorService) executor)::shutdown);
    }
    router.executor(name, executor);
    return this;
  }

  @NonNull @Override
  public Cookie getFlashCookie() {
    return router.getFlashCookie();
  }

  @NonNull @Override
  public Jooby setFlashCookie(@NonNull Cookie flashCookie) {
    router.setFlashCookie(flashCookie);
    return this;
  }

  @NonNull @Override
  public Jooby converter(@NonNull ValueConverter converter) {
    router.converter(converter);
    return this;
  }

  @NonNull @Override
  public List<ValueConverter> getConverters() {
    return router.getConverters();
  }

  @NonNull @Override
  public List<BeanConverter> getBeanConverters() {
    return router.getBeanConverters();
  }

  @NonNull @Override
  public Jooby setHiddenMethod(@NonNull Function<Context, Optional<String>> provider) {
    router.setHiddenMethod(provider);
    return this;
  }

  @NonNull @Override
  public Jooby setCurrentUser(@NonNull Function<Context, Object> provider) {
    router.setCurrentUser(provider);
    return this;
  }

  @NonNull @Override
  public Jooby setContextAsService(boolean contextAsService) {
    router.setContextAsService(contextAsService);
    return this;
  }

  @NonNull @Override
  public Jooby setHiddenMethod(@NonNull String parameterName) {
    router.setHiddenMethod(parameterName);
    return this;
  }

  /**
   * Controls the level of information logged during startup.
   *
   * @return Controls the level of information logged during startup.
   */
  public List<StartupSummary> getStartupSummary() {
    return startupSummary;
  }

  /**
   * Controls the level of information logged during startup.
   *
   * @param startupSummary Summary.
   * @return This instance.
   */
  public Jooby setStartupSummary(List<StartupSummary> startupSummary) {
    this.startupSummary = startupSummary;
    return this;
  }

  /**
   * Start application, find a web server, deploy application, start router, extension modules,
   * etc..
   *
   * @return Server.
   */
  public @NonNull Server start() {
    if (server == null) {
      this.server = loadServer();
    }
    if (!server.getLoggerOff().isEmpty()) {
      this.server = MutedServer.mute(this.server);
    }
    try {
      if (serverOptions == null) {
        serverOptions = ServerOptions.from(getEnvironment().getConfig()).orElse(null);
      }
      if (serverOptions != null) {
        serverOptions.setServer(server.getName());
        server.setOptions(serverOptions);
      }

      return server.start(this);
    } catch (Throwable startupError) {
      stopped.set(true);
      Logger log = getLog();
      log.error("Application startup resulted in exception", startupError);
      try {
        server.stop();
      } catch (Throwable stopError) {
        log.debug("Server stop resulted in exception", stopError);
      }
      // rethrow
      throw startupError instanceof StartupException
          ? (StartupException) startupError
          : new StartupException("Application startup resulted in exception", startupError);
    }
  }

  /**
   * Load server from classpath using {@link ServiceLoader}.
   *
   * @return A server.
   */
  private Server loadServer() {
    List<Server> servers =
        stream(
                spliteratorUnknownSize(
                    ServiceLoader.load(Server.class).iterator(), Spliterator.ORDERED),
                false)
            .toList();
    if (servers.isEmpty()) {
      throw new StartupException("Server not found.");
    }
    if (servers.size() > 1) {
      List<String> names =
          servers.stream()
              .map(it -> it.getClass().getSimpleName().toLowerCase())
              .collect(Collectors.toList());
      getLog().warn("Multiple servers found {}. Using: {}", names, names.get(0));
    }
    return servers.get(0);
  }

  /**
   * Call back method that indicates application was deploy it in the given server.
   *
   * @param server Server.
   * @return This application.
   */
  public @NonNull Jooby start(@NonNull Server server) {
    Path tmpdir = getTmpdir();
    ensureTmpdir(tmpdir);

    if (mode == null) {
      mode = ExecutionMode.DEFAULT;
    }

    if (locales == null) {
      String path = AvailableSettings.LANG;
      locales =
          Optional.of(getConfig())
              .filter(c -> c.hasPath(path))
              .map(c -> c.getString(path))
              .map(
                  v ->
                      LocaleUtils.parseLocales(v)
                          .orElseThrow(
                              () ->
                                  new RuntimeException(
                                      String.format(
                                          "Invalid value for configuration property '%s'; check the"
                                              + " documentation of %s#parse(): %s",
                                          path, Locale.LanguageRange.class.getName(), v))))
              .orElseGet(() -> singletonList(Locale.getDefault()));
    }

    ServiceRegistry services = getServices();
    services.put(Environment.class, getEnvironment());
    services.put(Config.class, getConfig());

    joobyRunHook(getClass().getClassLoader(), server);

    for (Extension extension : lateExtensions) {
      try {
        extension.install(this);
      } catch (Throwable e) {
        throw SneakyThrows.propagate(e);
      }
    }
    this.lateExtensions.clear();
    this.lateExtensions = null;

    this.startingCallbacks = fire(this.startingCallbacks);

    router.start(this);

    return this;
  }

  /**
   * Callback method that indicates application was successfully started it and listening for
   * connections.
   *
   * @param server Server.
   * @return This application.
   */
  public @NonNull Jooby ready(@NonNull Server server) {
    this.serverOptions = server.getOptions();

    if (startupSummary == null) {
      Config config = env.getConfig();
      if (config.hasPath(AvailableSettings.STARTUP_SUMMARY)) {
        Object value = config.getAnyRef(AvailableSettings.STARTUP_SUMMARY);
        List<String> values = value instanceof List ? (List) value : List.of(value.toString());
        startupSummary =
            values.stream().map(StartupSummary::create).collect(Collectors.toUnmodifiableList());
      } else {
        startupSummary = List.of(StartupSummary.DEFAULT, StartupSummary.ROUTES);
      }
    }
    startupSummary.forEach(summary -> summary.log(this, server));

    this.readyCallbacks = fire(this.readyCallbacks);
    return this;
  }

  /**
   * Stop application, fire the stop event to cleanup resources.
   *
   * <p>This method is usually invoked by {@link Server#stop()} using a shutdown hook.
   *
   * <p>The next example shows how to successfully stop the web server and application:
   *
   * <pre>{@code
   * Jooby app = new Jooby();
   *
   * Server server = app.start();
   *
   * ...
   *
   * server.stop();
   * }</pre>
   *
   * @return This application.
   */
  public @NonNull Jooby stop() {
    if (started.compareAndSet(true, false)) {
      stopped.set(true);
      Logger log = getLog();
      log.debug("Stopping {}", System.getProperty(APP_NAME, getClass().getSimpleName()));
      router.destroy();

      fireStop();

      log.info("Stopped {}", System.getProperty(APP_NAME, getClass().getSimpleName()));
    }
    return this;
  }

  /**
   * Force all module to be initialized lazily at application startup time (not at
   * creation/instantiation time).
   *
   * <p>This option is present mostly for unit-test where you need to instantiated a Jooby instance
   * without running extensions.
   *
   * @param lateInit True for late init.
   * @return This application.
   */
  public Jooby setLateInit(boolean lateInit) {
    this.lateInit = lateInit;
    return this;
  }

  /**
   * Get application's name. If none set:
   *
   * <p>- Try to get from {@link Package#getImplementationTitle()}. - Otherwise fallback to class
   * name.
   *
   * @return Application's name.
   */
  public @NonNull String getName() {
    if (name == null) {
      name = System.getProperty(APP_NAME);
      if (name == null) {
        name =
            Optional.ofNullable(getClass().getPackage())
                .map(Package::getImplementationTitle)
                .filter(Objects::nonNull)
                .orElse(getClass().getSimpleName());
      }
    }
    return name;
  }

  /**
   * Set application name.
   *
   * @param name Application's name.
   * @return This application.
   */
  public @NonNull Jooby setName(@NonNull String name) {
    this.name = name;
    return this;
  }

  /**
   * Get application version (description/debugging purpose only). If none set:
   *
   * <p>- Try to get it from {@link Package#getImplementationVersion()}. This attribute is present
   * when application has been packaged (jar file).
   *
   * <p>- Otherwise, fallback to <code>0.0.0</code>.
   *
   * @return Application version.
   */
  public @NonNull String getVersion() {
    if (version == null) {
      version =
          Optional.ofNullable(getClass().getPackage())
              .map(Package::getImplementationVersion)
              .filter(Objects::nonNull)
              .orElse("0.0.0");
    }
    return version;
  }

  /**
   * Set application version.
   *
   * @param version Application's version.
   * @return This application.
   */
  public @NonNull Jooby setVersion(@NonNull String version) {
    this.version = version;
    return this;
  }

  @Override
  public String toString() {
    return getName() + ":" + getVersion();
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param provider Application provider.
   */
  public static void runApp(@NonNull String[] args, @NonNull Supplier<Jooby> provider) {
    runApp(args, ExecutionMode.DEFAULT, provider);
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param consumer Application consumer.
   */
  public static void runApp(@NonNull String[] args, @NonNull Consumer<Jooby> consumer) {
    configurePackage(consumer.getClass().getPackage());
    runApp(args, ExecutionMode.DEFAULT, consumerProvider(consumer));
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param executionMode Application execution mode.
   * @param consumer Application consumer.
   */
  public static void runApp(
      @NonNull String[] args,
      @NonNull ExecutionMode executionMode,
      @NonNull Consumer<Jooby> consumer) {
    configurePackage(consumer.getClass().getPackage());
    runApp(args, executionMode, consumerProvider(consumer));
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param executionMode Application execution mode.
   * @param provider Application provider.
   */
  public static void runApp(
      @NonNull String[] args,
      @NonNull ExecutionMode executionMode,
      @NonNull Supplier<Jooby> provider) {
    createApp(args, executionMode, provider).start();
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param executionMode Application execution mode.
   * @param provider Application provider.
   * @return Application.
   */
  public static Jooby createApp(
      @NonNull String[] args,
      @NonNull ExecutionMode executionMode,
      @NonNull Supplier<Jooby> provider) {

    configurePackage(provider.getClass().getPackage());

    /** Dump command line as system properties. */
    parseArguments(args).forEach(System::setProperty);

    /** Find application.env: */
    String logfile =
        LoggingService.configure(
            provider.getClass().getClassLoader(), new EnvironmentOptions().getActiveNames());
    if (logfile != null) {
      // Add as property, so we can query where is the log configuration
      System.setProperty(AvailableSettings.LOG_FILE, logfile);
    }

    Jooby app;
    try {
      app = provider.get();
    } catch (Throwable t) {
      LoggerFactory.getLogger(Jooby.class)
          .error("Application initialization resulted in exception", t);

      throw t instanceof StartupException
          ? (StartupException) t
          : new StartupException("Application initialization resulted in exception", t);
    }

    if (app.mode == null) {
      app.mode = executionMode;
    }
    return app;
  }

  private static void configurePackage(Package pkg) {
    if (pkg != null) {
      configurePackage(pkg.getName());
    }
  }

  private static void configurePackage(Class owner) {
    if (!owner.getName().equals("io.jooby.Jooby") && !owner.getName().equals("io.jooby.kt.Kooby")) {
      configurePackage(owner.getPackage());
    }
  }

  private static void configurePackage(String packageName) {
    var defaultPackages = Set.of("io.jooby", "io.jooby.kt");
    if (!defaultPackages.contains(packageName)) {
      ifSystemProp(
          AvailableSettings.PACKAGE,
          (sys, key) -> {
            sys.setProperty(key, packageName);
          });
    }
  }

  private static void ifSystemProp(String name, BiConsumer<Properties, String> consumer) {
    if (System.getProperty(name) == null) {
      consumer.accept(System.getProperties(), name);
    }
  }

  static Map<String, String> parseArguments(String... args) {
    if (args == null || args.length == 0) {
      return Collections.emptyMap();
    }
    Map<String, String> conf = new LinkedHashMap<>();
    for (String arg : args) {
      int eq = arg.indexOf('=');
      if (eq > 0) {
        conf.put(arg.substring(0, eq).trim(), arg.substring(eq + 1).trim());
      } else {
        // must be the environment actives
        conf.putIfAbsent(AvailableSettings.ENV, arg);
      }
    }
    return conf;
  }

  private static void ensureTmpdir(Path tmpdir) {
    try {
      if (!Files.exists(tmpdir)) {
        Files.createDirectories(tmpdir);
      }
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private List<SneakyThrows.Runnable> fire(List<SneakyThrows.Runnable> tasks) {
    if (tasks != null) {
      Iterator<SneakyThrows.Runnable> iterator = tasks.iterator();
      while (iterator.hasNext()) {
        SneakyThrows.Runnable task = iterator.next();
        task.run();
        iterator.remove();
      }
    }
    return null;
  }

  private void fireStop() {
    if (stopCallbacks != null) {
      List<AutoCloseable> tasks = stopCallbacks;
      stopCallbacks = null;
      Iterator<AutoCloseable> iterator = tasks.iterator();
      while (iterator.hasNext()) {
        AutoCloseable task = iterator.next();
        try {
          task.close();
        } catch (Exception x) {
          getLog().error("exception found while executing onStop:", x);
        }
        iterator.remove();
      }
    }
  }

  private static Supplier<Jooby> consumerProvider(Consumer<Jooby> consumer) {
    configurePackage(consumer.getClass());
    return () -> {
      Jooby app = new Jooby();
      consumer.accept(app);
      return app;
    };
  }

  /**
   * Check if we are running from joobyRun and invoke a hook class passing the web server. JoobyRun
   * intercept the server and use it for doing hotswap.
   *
   * @param loader Class loader.
   * @param server Server.
   */
  private void joobyRunHook(ClassLoader loader, Server server) {
    if (loader.getClass().getName().equals("org.jboss.modules.ModuleClassLoader")) {
      String hookClassname = System.getProperty(JOOBY_RUN_HOOK);
      System.setProperty(JOOBY_RUN_HOOK, "");
      if (hookClassname != null && hookClassname.length() > 0) {
        try {
          Class serverRefClass = loader.loadClass(hookClassname);
          Constructor constructor = serverRefClass.getDeclaredConstructor();
          Consumer<Server> consumer = (Consumer<Server>) constructor.newInstance();
          consumer.accept(MutedServer.mute(server));
        } catch (Exception x) {
          throw SneakyThrows.propagate(x);
        }
      }
    }
  }

  /**
   * This method exists to integrate IDE incremental build with MVC annotation processor. It
   * fallback to reflection to lookup for a generated mvc factory.
   *
   * @param source Controller class.
   * @param classLoader Class loader.
   * @return Mvc factory.
   */
  private MvcFactory mvcReflectionFallback(Class source, ClassLoader classLoader) {
    try {
      String moduleName = source.getName() + "$Module";
      Class<?> moduleType = classLoader.loadClass(moduleName);
      Constructor<?> constructor = moduleType.getDeclaredConstructor();
      getLog().debug("Loading mvc using reflection: " + source);
      return (MvcFactory) constructor.newInstance();
    } catch (Exception x) {
      throw Usage.mvcRouterNotFound(source);
    }
  }

  /**
   * Copy internal state from one application into other.
   *
   * @param source Source application.
   * @param dest Destination application.
   */
  private static void copyState(Jooby source, Jooby dest) {
    dest.serverOptions = source.serverOptions;
    dest.registry = source.registry;
    dest.mode = source.mode;
    dest.environmentOptions = source.environmentOptions;
    dest.name = source.name;
    dest.version = source.version;
    dest.lateInit = source.lateInit;
    dest.locales = source.locales;
    dest.env = source.getEnvironment();
    dest.router = source.router;
    dest.lateExtensions = source.lateExtensions;
    dest.readyCallbacks = source.readyCallbacks;
    dest.startingCallbacks = source.startingCallbacks;
    dest.stopCallbacks = source.stopCallbacks;
  }
}
