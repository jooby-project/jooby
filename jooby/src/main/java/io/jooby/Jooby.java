/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.exception.RegistryException;
import io.jooby.exception.StartupException;
import io.jooby.internal.LocaleUtils;
import io.jooby.internal.MutedServer;
import io.jooby.internal.RegistryRef;
import io.jooby.internal.RouterImpl;
import io.jooby.output.OutputFactory;
import io.jooby.problem.ProblemDetailsHandler;
import io.jooby.value.ValueFactory;

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
  private static final Logger log = LoggerFactory.getLogger(Jooby.class);

  private final transient AtomicBoolean started = new AtomicBoolean(true);

  private final transient AtomicBoolean stopped = new AtomicBoolean(false);

  private static Jooby owner;
  private static ExecutionMode BOOT_EXECUTION_MODE = ExecutionMode.DEFAULT;
  private static OutputFactory OUTPUT_FACTORY;
  private static ServerOptions SERVER_OPTIONS;

  private RouterImpl router;

  private ExecutionMode mode;

  private Path tmpdir;

  private List<SneakyThrows.Runnable> readyCallbacks;

  private List<SneakyThrows.Runnable> startingCallbacks;

  private LinkedList<AutoCloseable> stopCallbacks;

  private List<Extension> lateExtensions;

  private Environment env;

  private RegistryRef registry = new RegistryRef();

  private List<StartupSummary> startupSummary;

  private EnvironmentOptions environmentOptions;

  private List<Locale> locales;

  private boolean lateInit;

  private String name;

  private String basePackage;

  private String version;

  /** Creates a new Jooby instance. */
  public Jooby() {
    if (owner == null) {
      ClassLoader classLoader = getClass().getClassLoader();
      mode = BOOT_EXECUTION_MODE;
      environmentOptions = new EnvironmentOptions().setClassLoader(classLoader);
      router = new RouterImpl();
      stopCallbacks = new LinkedList<>();
      startingCallbacks = new ArrayList<>();
      readyCallbacks = new ArrayList<>();
      lateExtensions = new ArrayList<>();
      // NOTE: fallback to default, this is required for direct instance creation of class
      // app bootstrap always ensures server instance.
      router.setOutputFactory(Optional.ofNullable(OUTPUT_FACTORY).orElseGet(OutputFactory::create));
      router.setServerOptions(Optional.ofNullable(SERVER_OPTIONS).orElseGet(ServerOptions::new));
    } else {
      copyState(owner, this);
    }
  }

  @NonNull @Override
  public RouterOptions getRouterOptions() {
    return router.getRouterOptions();
  }

  @NonNull public Jooby setRouterOptions(@NonNull RouterOptions options) {
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
   * @return Created routes.
   */
  @NonNull public Route.Set install(@NonNull SneakyThrows.Supplier<Jooby> factory) {
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
   * @return Created routes.
   */
  @NonNull public Route.Set install(@NonNull String path, @NonNull SneakyThrows.Supplier<Jooby> factory) {
    try {
      owner = this;
      return path(path, factory::get);
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

  @NonNull @Override
  public ServerOptions getServerOptions() {
    return router.getServerOptions();
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
  public Route.Set domain(@NonNull String domain, @NonNull Router subrouter) {
    return this.router.domain(domain, subrouter);
  }

  @NonNull @Override
  public Route.Set domain(@NonNull String domain, @NonNull Runnable body) {
    return router.domain(domain, body);
  }

  @NonNull @Override
  public Route.Set mount(@NonNull Predicate<Context> predicate, @NonNull Runnable body) {
    return router.mount(predicate, body);
  }

  @NonNull @Override
  public Route.Set mount(@NonNull Predicate<Context> predicate, @NonNull Router subrouter) {
    return this.router.mount(predicate, subrouter);
  }

  @NonNull @Override
  public Route.Set mount(@NonNull String path, @NonNull Router router) {
    var rs = this.router.mount(path, router);
    if (router instanceof Jooby) {
      Jooby child = (Jooby) router;
      child.registry = this.registry;
    }
    return rs;
  }

  @NonNull @Override
  public Route.Set mount(@NonNull Router router) {
    return mount("/", router);
  }

  /**
   * Add controller routes.
   *
   * @param router Mvc extension.
   * @return Route set.
   */
  @NonNull public Route.Set mvc(@NonNull Extension router) {
    try {
      int start = this.router.getRoutes().size();
      router.install(this);
      return new Route.Set(this.router.getRoutes().subList(start, this.router.getRoutes().size()));
    } catch (Exception cause) {
      throw SneakyThrows.propagate(cause);
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
  public Route.Set path(@NonNull String pattern, @NonNull Runnable action) {
    return router.path(pattern, action);
  }

  @NonNull @Override
  public Route.Set routes(@NonNull Runnable action) {
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
  public Logger getLog() {
    return LoggerFactory.getLogger(getClass());
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
    return mode;
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
  public Jooby setAttribute(@NonNull String key, @NonNull Object value) {
    router.setAttribute(key, value);
    return this;
  }

  @NonNull @Override
  public <T> T getAttribute(@NonNull String key) {
    return router.getAttribute(key);
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type, @NonNull String name) {
    return require(ServiceKey.key(type, name));
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type) {
    return require(ServiceKey.key(type));
  }

  @NonNull @Override
  public <T> T require(@NonNull Reified<T> type) throws RegistryException {
    return require(ServiceKey.key(type));
  }

  @NonNull @Override
  public <T> T require(@NonNull Reified<T> type, @NonNull String name) throws RegistryException {
    return require(ServiceKey.key(type, name));
  }

  @Override
  public @NonNull <T> T require(@NonNull ServiceKey<T> key) {
    ServiceRegistry services = getServices();
    T service = services.getOrNull(key);
    if (service == null) {
      if (!registry.isSet()) {
        throw new RegistryException("Service not found: " + key);
      }
      return registry.get().require(key);
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
  public ValueFactory getValueFactory() {
    return router.getValueFactory();
  }

  @NonNull @Override
  public Jooby setValueFactory(@NonNull ValueFactory valueFactory) {
    router.setValueFactory(valueFactory);
    return this;
  }

  @NonNull @Override
  public OutputFactory getOutputFactory() {
    return router.getOutputFactory();
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
   * Call back method that indicates application was deployed.
   *
   * @param server Server.
   * @return This application.
   */
  public @NonNull Jooby start(@NonNull Server server) {
    Path tmpdir = getTmpdir();
    ensureTmpdir(tmpdir);

    log.trace("initialization context static variables {} {}", Context.RFC1123, Context.GMT);

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

    var services = getServices();
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

    router.start(this, server);

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
    if (startupSummary == null) {
      Config config = env.getConfig();
      if (config.hasPath(AvailableSettings.STARTUP_SUMMARY)) {
        Object value = config.getAnyRef(AvailableSettings.STARTUP_SUMMARY);
        List<String> values = value instanceof List ? (List) value : List.of(value.toString());
        startupSummary = values.stream().map(StartupSummary::create).toList();
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
   * @param provider Application provider.
   */
  public static void runApp(
      @NonNull String[] args, @NonNull Server server, @NonNull Supplier<Jooby> provider) {
    configurePackage(provider.getClass().getPackage());
    runApp(args, server, List.of(provider));
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
   * @param server Server to run.
   * @param consumer Application consumer.
   */
  public static void runApp(
      @NonNull String[] args, @NonNull Server server, @NonNull Consumer<Jooby> consumer) {
    runApp(args, server, ExecutionMode.DEFAULT, consumer);
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param server Server to run.
   * @param executionMode Default application execution mode. Can be overridden by application.
   * @param consumer Application consumer.
   */
  public static void runApp(
      @NonNull String[] args,
      @NonNull Server server,
      @NonNull ExecutionMode executionMode,
      @NonNull Consumer<Jooby> consumer) {
    configurePackage(consumer.getClass().getPackage());
    runApp(args, server, executionMode, List.of(consumerProvider(consumer)));
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param executionMode Default application execution mode. Can be overridden by application.
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
   * @param executionMode Default application execution mode. Can be overridden by application.
   * @param provider Application provider.
   */
  public static void runApp(
      @NonNull String[] args,
      @NonNull ExecutionMode executionMode,
      @NonNull Supplier<Jooby> provider) {
    runApp(args, executionMode, List.of(provider));
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param executionMode Default application execution mode. Can be overridden by application.
   * @param provider Application provider.
   */
  public static void runApp(
      @NonNull String[] args,
      @NonNull Server server,
      @NonNull ExecutionMode executionMode,
      @NonNull Supplier<Jooby> provider) {
    configurePackage(provider.getClass().getPackage());
    runApp(args, server, executionMode, List.of(provider));
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param provider Application provider.
   */
  public static void runApp(@NonNull String[] args, @NonNull List<Supplier<Jooby>> provider) {
    runApp(args, ExecutionMode.DEFAULT, provider);
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param executionMode Default application execution mode. Can be overridden by application.
   * @param provider Application provider.
   */
  public static void runApp(
      @NonNull String[] args,
      @NonNull ExecutionMode executionMode,
      @NonNull List<Supplier<Jooby>> provider) {
    var cmd = parseArguments(args);
    var options = ServerOptions.from(ConfigFactory.parseMap(cmd)).orElse(null);
    runApp(args, Server.loadServer(), options, executionMode, provider);
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param server Server.
   * @param provider Application provider.
   */
  public static void runApp(
      @NonNull String[] args, @NonNull Server server, @NonNull List<Supplier<Jooby>> provider) {
    runApp(args, server, ExecutionMode.DEFAULT, provider);
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param server Server.
   * @param executionMode Default application execution mode. Can be overridden by application.
   * @param provider Application provider.
   */
  public static void runApp(
      @NonNull String[] args,
      @NonNull Server server,
      @NonNull ExecutionMode executionMode,
      @NonNull List<Supplier<Jooby>> provider) {
    runApp(args, server, null, executionMode, provider);
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param server Server.
   * @param executionMode Default application execution mode. Can be overridden by application.
   * @param provider Application provider.
   */
  private static void runApp(
      @NonNull String[] args,
      @NonNull Server server,
      @Nullable ServerOptions options,
      @NonNull ExecutionMode executionMode,
      @NonNull List<Supplier<Jooby>> provider) {

    /* Dump command line as system properties. */
    parseArguments(args).forEach(System::setProperty);
    var apps = new ArrayList<Jooby>();
    var targetServer = server.getLoggerOff().isEmpty() ? server : MutedServer.mute(server);
    try {
      for (var factory : provider) {
        var app = createApp(server, executionMode, factory);
        /*
         When running a single app instance, there is no issue with server options.
         When multiple apps pick first on the provided order.
        */
        if (options == null) {
          options = ServerOptions.from(app.getConfig()).orElse(null);
        }
        apps.add(app);
      }
      if (options != null) {
        server.setOptions(options);
      }
      targetServer.start(apps.toArray(new Jooby[0]));
    } catch (Throwable startupError) {
      try {
        targetServer.stop();
      } catch (Throwable ignored) {
        // no need to log here
      }
      // rethrow
      throw startupError instanceof StartupException
          ? (StartupException) startupError
          : new StartupException("Application initialization resulted in exception", startupError);
    }
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param executionMode Application execution mode.
   * @param provider Application provider.
   * @return Application.
   */
  public static Jooby createApp(
      @NonNull Server server,
      @NonNull ExecutionMode executionMode,
      @NonNull Supplier<Jooby> provider) {
    configurePackage(provider.getClass().getPackage());
    /* Find application.env: */
    String logfile =
        LoggingService.configure(
            provider.getClass().getClassLoader(), new EnvironmentOptions().getActiveNames());
    if (logfile != null) {
      // Add as property, so we can query where is the log configuration
      System.setProperty(AvailableSettings.LOG_FILE, logfile);
    }

    Jooby app;
    try {
      Jooby.OUTPUT_FACTORY = server.getOutputFactory();
      Jooby.SERVER_OPTIONS = server.getOptions();
      Jooby.BOOT_EXECUTION_MODE = executionMode;
      app = provider.get();
    } finally {
      Jooby.BOOT_EXECUTION_MODE = executionMode;
      Jooby.OUTPUT_FACTORY = null;
      Jooby.SERVER_OPTIONS = null;
    }

    return app;
  }

  /**
   * Check if {@link ProblemDetailsHandler} is enabled as a global error handler
   *
   * @return boolean flag
   */
  public boolean problemDetailsIsEnabled() {
    var config = getConfig();
    return config.hasPath(ProblemDetailsHandler.ENABLED_KEY)
        && config.getBoolean(ProblemDetailsHandler.ENABLED_KEY);
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
    var defaultPackages = java.util.Set.of("io.jooby", "io.jooby.kt");
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
      return Map.of();
    }
    var commandLine = new LinkedHashMap<String, String>();
    for (var arg : args) {
      int eq = arg.indexOf('=');
      if (eq > 0) {
        commandLine.put(arg.substring(0, eq).trim(), arg.substring(eq + 1).trim());
      } else {
        // must be the environment actives
        commandLine.putIfAbsent(AvailableSettings.ENV, arg);
      }
    }
    return commandLine;
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
   * Copy the internal state from one application into others.
   *
   * @param source Source application.
   * @param dest Destination application.
   */
  private static void copyState(Jooby source, Jooby dest) {
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
