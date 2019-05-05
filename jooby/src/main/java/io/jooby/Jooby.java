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
package io.jooby;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.jooby.internal.RouterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

/**
 * <h1>Welcome to Jooby!</h1>
 *
 * <p>Hello World!</p>
 * <pre>{@code
 *
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
 * @since 2.0.0
 * @author edgar
 */
public class Jooby implements Router, Registry {

  static final String DEF_PCKG = "___def_package__";

  static final String APP_NAME = "___app_name__";

  private RouterImpl router;

  private ExecutionMode mode;

  private Path tmpdir;

  private List<Throwing.Runnable> readyCallbacks;

  private LinkedList<AutoCloseable> stopCallbacks;

  private Environment env;

  private Registry registry;

  private ServerOptions serverOptions;

  private EnvironmentOptions environmentOptions;

  /**
   * Creates a new Jooby instance.
   */
  public Jooby() {
    ClassLoader classLoader = getClass().getClassLoader();
    environmentOptions = new EnvironmentOptions().setClassLoader(classLoader);
    router = new RouterImpl(classLoader);
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
  public @Nonnull Jooby setServerOptions(@Nonnull ServerOptions serverOptions) {
    this.serverOptions = serverOptions;
    return this;
  }

  @Nonnull @Override public RouterOptions getRouterOptions() {
    return router.getRouterOptions();
  }

  @Nonnull @Override public Jooby setRouterOptions(@Nonnull RouterOptions options) {
    router.setRouterOptions(options);
    return this;
  }

  /**
   * Application environment. If none was set, environment is initialized
   * using {@link Environment#loadEnvironment(EnvironmentOptions)}.
   *
   * @return Application environment.
   */
  public @Nonnull Environment getEnvironment() {
    if (env == null) {
      env = Environment.loadEnvironment(environmentOptions);
    }
    return env;
  }

  /**
   * Application configuration. It is a shortcut for {@link Environment#getConfig()}.
   *
   * @return Application config.
   */
  public @Nonnull Config getConfig() {
    return getEnvironment().getConfig();
  }

  /**
   * Set application environment.
   *
   * @param environment Application environment.
   * @return This application.
   */
  public @Nonnull Jooby setEnvironment(@Nonnull Environment environment) {
    this.env = environment;
    return this;
  }

  /**
   * Set environment options and initialize/overrides the environment.
   *
   * @param options Environment options.
   * @return New environment.
   */
  public @Nonnull Environment setEnvironmentOptions(@Nonnull EnvironmentOptions options) {
    this.environmentOptions = options;
    this.env = Environment.loadEnvironment(
        options.setClassLoader(options.getClassLoader(getClass().getClassLoader())));
    return this.env;
  }

  /**
   * Start event is fire once all components has been initialized, for example router and web-server
   * are up and running, extension installed, etc...
   *
   * @param body Start body.
   * @return This application.
   */
  public @Nonnull Jooby onStart(@Nonnull Throwing.Runnable body) {
    if (readyCallbacks == null) {
      readyCallbacks = new ArrayList<>();
    }
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
  public @Nonnull Jooby onStop(@Nonnull AutoCloseable body) {
    if (stopCallbacks == null) {
      stopCallbacks = new LinkedList<>();
    }
    stopCallbacks.addFirst(body);
    return this;
  }

  @Nonnull @Override public Jooby setContextPath(@Nonnull String basePath) {
    router.setContextPath(basePath);
    return this;
  }

  @Nonnull @Override public String getContextPath() {
    return router.getContextPath();
  }

  @Nonnull @Override
  public Jooby use(@Nonnull Router router) {
    this.router.use(router);
    return this;
  }

  @Nonnull @Override
  public Jooby use(@Nonnull Predicate<Context> predicate, @Nonnull Router router) {
    this.router.use(predicate, router);
    return this;
  }

  @Nonnull @Override public Jooby use(@Nonnull String path, @Nonnull Router router) {
    this.router.use(path, router);
    return this;
  }

  @Nonnull @Override public Jooby mvc(@Nonnull Object router) {
    this.router.mvc(router);
    return this;
  }

  @Nonnull @Override public Jooby mvc(@Nonnull Class router) {
    this.router.mvc(router, () -> require(router));
    return this;
  }

  @Nonnull @Override
  public <T> Jooby mvc(@Nonnull Class<T> router, @Nonnull Provider<T> provider) {
    this.router.mvc(router, provider);
    return this;
  }

  @Nonnull @Override public List<Route> getRoutes() {
    return router.getRoutes();
  }

  @Nonnull @Override public Jooby error(@Nonnull ErrorHandler handler) {
    router.error(handler);
    return this;
  }

  @Nonnull @Override public Jooby decorator(@Nonnull Route.Decorator decorator) {
    router.decorator(decorator);
    return this;
  }

  @Nonnull @Override public Jooby before(@Nonnull Route.Before before) {
    router.before(before);
    return this;
  }

  @Nonnull @Override public Jooby after(@Nonnull Route.After after) {
    router.after(after);
    return this;
  }

  @Nonnull @Override public Jooby renderer(@Nonnull Renderer renderer) {
    router.renderer(renderer);
    return this;
  }

  @Nonnull @Override public Jooby parser(@Nonnull MediaType contentType, @Nonnull Parser parser) {
    router.parser(contentType, parser);
    return this;
  }

  @Nonnull @Override
  public Jooby renderer(@Nonnull MediaType contentType, @Nonnull Renderer renderer) {
    router.renderer(contentType, renderer);
    return this;
  }

  /**
   * Install extension module.
   *
   * @param extension Extension module.
   * @return This application.
   */
  @Nonnull public Jooby install(@Nonnull Extension extension) {
    try {
      extension.install(this);
      return this;
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public Jooby dispatch(@Nonnull Runnable body) {
    router.dispatch(body);
    return this;
  }

  @Nonnull @Override public Jooby dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    router.dispatch(executor, action);
    return this;
  }

  @Nonnull @Override public Jooby path(@Nonnull String pattern, @Nonnull Runnable action) {
    router.path(pattern, action);
    return this;
  }

  @Nonnull @Override public Jooby route(@Nonnull Runnable action) {
    router.route(action);
    return this;
  }

  @Nonnull @Override
  public Route route(@Nonnull String method, @Nonnull String pattern,
      @Nonnull Route.Handler handler) {
    return router.route(method, pattern, handler);
  }

  @Nonnull @Override public Match match(@Nonnull Context ctx) {
    return router.match(ctx);
  }

  @Nonnull @Override
  public Jooby errorCode(@Nonnull Class<? extends Throwable> type,
      @Nonnull StatusCode statusCode) {
    router.errorCode(type, statusCode);
    return this;
  }

  @Nonnull @Override public StatusCode errorCode(@Nonnull Throwable cause) {
    return router.errorCode(cause);
  }

  @Nonnull @Override public Executor getWorker() {
    return router.getWorker();
  }

  @Nonnull @Override public Jooby setWorker(@Nonnull Executor worker) {
    this.router.setWorker(worker);
    if (worker instanceof ExecutorService) {
      onStop(((ExecutorService) worker)::shutdown);
    }
    return this;
  }

  @Nonnull @Override public Jooby setDefaultWorker(@Nonnull Executor worker) {
    this.router.setDefaultWorker(worker);
    return this;
  }

  @Nonnull @Override public Logger getLog() {
    return LoggerFactory.getLogger(getClass());
  }

  @Nonnull @Override public Jooby responseHandler(ResponseHandler handler) {
    router.responseHandler(handler);
    return this;
  }

  @Nonnull @Override public ErrorHandler getErrorHandler() {
    return router.getErrorHandler();
  }

  @Nonnull @Override public Path getTmpdir() {
    return tmpdir;
  }

  /**
   * Set application temporary directory.
   *
   * @param tmpdir Temp directory.
   * @return This application.
   */
  public @Nonnull Jooby setTmpdir(@Nonnull Path tmpdir) {
    this.tmpdir = tmpdir;
    return this;
  }

  /**
   * Application execution mode.
   *
   * @return Application execution mode.
   */
  public @Nonnull ExecutionMode getExecutionMode() {
    return mode == null ? ExecutionMode.DEFAULT : mode;
  }

  /**
   * Set application execution mode.
   *
   * @param mode Application execution mode.
   * @return This application.
   */
  public @Nonnull Jooby setExecutionMode(@Nonnull ExecutionMode mode) {
    this.mode = mode;
    return this;
  }

  @Nonnull @Override public Map<String, Object> getAttributes() {
    return router.getAttributes();
  }

  @Nonnull @Override public Jooby attribute(@Nonnull String key, Object value) {
    router.attribute(key, value);
    return this;
  }

  @Nonnull @Override public <T> T attribute(@Nonnull String key) {
    return router.attribute(key);
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type, @Nonnull String name) {
    return checkRegistry().require(type, name);
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type) {
    return checkRegistry().require(type);
  }

  /**
   * Set application registry.
   *
   * @param registry Application registry.
   * @return This application.
   */
  @Nonnull public Jooby registry(@Nonnull Registry registry) {
    this.registry = registry;
    return this;
  }

  @Nonnull @Override public Map<ResourceKey, Object> getResources() {
    return this.router.getResources();
  }

  @Nonnull @Override public <R> Jooby resource(@Nonnull ResourceKey<R> key, @Nonnull R resource) {
    this.router.resource(key, resource);
    return this;
  }

  @Nonnull @Override public <T> T resource(@Nonnull ResourceKey<T> key)
      throws IllegalStateException {
    return this.router.resource(key);
  }

  private Registry checkRegistry() {
    if (registry == null) {
      throw new IllegalStateException("No registry available");
    }
    return registry;
  }

  /**
   * Get base application package. This is the package from where application was initialized
   * or the package of a Jooby application sub-class.
   *
   * @return Base application package.
   */
  public @Nullable String getBasePackage() {
    String classname = getClass().getName();
    if (classname.equals("io.jooby.Jooby") || classname.equals("io.jooby.Kooby")) {
      return System.getProperty(DEF_PCKG);
    }
    return getClass().getPackage().getName();
  }

  @Nonnull @Override public SessionOptions getSessionOptions() {
    return router.getSessionOptions();
  }

  @Nonnull @Override public Jooby setSessionOptions(@Nonnull SessionOptions options) {
    router.setSessionOptions(options);
    return this;
  }

  /**
   * Start application, find a web server, deploy application, start router, extension modules,
   * etc..
   *
   * @return Server.
   */
  public @Nonnull Server start() {
    List<Server> servers = stream(
        spliteratorUnknownSize(
            ServiceLoader.load(Server.class).iterator(),
            Spliterator.ORDERED),
        false)
        .collect(Collectors.toList());
    if (servers.size() == 0) {
      throw new IllegalStateException("Server not found.");
    }
    if (servers.size() > 1) {
      List<String> names = servers.stream()
          .map(it -> it.getClass().getSimpleName().toLowerCase())
          .collect(Collectors.toList());
      getLog().warn("Multiple servers found {}. Using: {}", names, names.get(0));
    }
    Server server = servers.get(0);
    try {
      if (serverOptions == null) {
        serverOptions = ServerOptions.from(getEnvironment().getConfig()).orElse(null);
      }
      if (serverOptions != null) {
        serverOptions.setServer(server.getClass().getSimpleName().toLowerCase());
        server.setOptions(serverOptions);
      }
      return server.start(this);
    } catch (Throwable x) {
      Logger log = getLog();
      log.error("Application startup resulted in exception", x);
      try {
        server.stop();
      } catch (Throwable stopx) {
        log.info("Server stop resulted in exception", stopx);
      }
      // rethrow
      throw Throwing.sneakyThrow(x);
    }
  }

  /**
   * Call back method that indicates application was deploy it in the given server.
   *
   * @param server Server.
   * @return This application.
   */
  public @Nonnull Jooby start(@Nonnull Server server) {
    Environment env = getEnvironment();
    if (tmpdir == null) {
      tmpdir = Paths.get(env.getConfig().getString("application.tmpdir")).toAbsolutePath();
    }

    /** Start router: */
    ensureTmpdir(tmpdir);

    if (mode == null) {
      mode = ExecutionMode.DEFAULT;
    }
    router.start(this);

    return this;
  }

  /**
   * Callback method that indicates application was successfully started it and it is ready
   * listening for connections.
   *
   * @param server Server.
   * @return This application.
   */
  public @Nonnull Jooby ready(@Nonnull Server server) {
    Logger log = getLog();

    fireStarted();

    log.info("{} started with:", System.getProperty(APP_NAME, getClass().getSimpleName()));

    log.info("    PID: {}", System.getProperty("PID"));
    log.info("    {}", server.getOptions());
    if (log.isDebugEnabled()) {
      log.debug("    env: {}", env);
    } else {
      log.info("    env: {}", env.getActiveNames());
    }
    log.info("    execution model: {}", mode.name().toLowerCase());
    log.info("    user: {}", System.getProperty("user.name"));
    log.info("    app dir: {}", System.getProperty("user.dir"));
    log.info("    tmp dir: {}", tmpdir);

    log.info("routes: \n\n{}\n\nlistening on:\n  http://localhost:{}{}\n", router,
        server.getOptions().getPort(),
        router.getContextPath());
    return this;
  }

  /**
   * Stop application, fire the stop event to cleanup resources.
   *
   * @return This application.
   */
  public @Nonnull Jooby stop() {
    Logger log = getLog();
    log.debug("Stopping {}", System.getProperty(APP_NAME, getClass().getSimpleName()));
    if (router != null) {
      router.destroy();
      router = null;
    }

    fireStop();

    log.info("Stopped {}", System.getProperty(APP_NAME, getClass().getSimpleName()));
    return this;
  }

  @Override public String toString() {
    return router.toString();
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param applicationType Application type.
   */
  public static void runApp(@Nonnull String[] args,
      @Nonnull Class<? extends Jooby> applicationType) {
    runApp(args, ExecutionMode.DEFAULT, applicationType);
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param executionMode Application execution mode.
   * @param applicationType Application type.
   */
  public static void runApp(@Nonnull String[] args, @Nonnull ExecutionMode executionMode,
      @Nonnull Class<? extends Jooby> applicationType) {
    configurePackage(applicationType);
    runApp(args, executionMode, reflectionProvider(applicationType));
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param executionMode Application execution mode.
   * @param applicationType Application type.
   * @return Application.
   */
  public static Jooby createApp(@Nonnull String[] args, @Nonnull ExecutionMode executionMode,
      @Nonnull Class<? extends Jooby> applicationType) {
    configurePackage(applicationType);
    return createApp(args, executionMode, reflectionProvider(applicationType));
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param provider Application provider.
   */
  public static void runApp(@Nonnull String[] args, @Nonnull Supplier<Jooby> provider) {
    runApp(args, ExecutionMode.DEFAULT, provider);
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param consumer Application consumer.
   */
  public static void runApp(@Nonnull String[] args, @Nonnull Consumer<Jooby> consumer) {
    runApp(args, ExecutionMode.DEFAULT, consumerProvider(consumer));
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param executionMode Application execution mode.
   * @param consumer Application consumer.
   */
  public static void runApp(@Nonnull String[] args, @Nonnull ExecutionMode executionMode,
      @Nonnull Consumer<Jooby> consumer) {
    runApp(args, executionMode, consumerProvider(consumer));
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param executionMode Application execution mode.
   * @param provider Application provider.
   */
  public static void runApp(@Nonnull String[] args, @Nonnull ExecutionMode executionMode,
      @Nonnull Supplier<Jooby> provider) {
    Jooby app = createApp(args, executionMode, provider);
    Server server = app.start();
    server.join();
  }

  /**
   * Setup default environment, logging (logback or log4j2) and run application.
   *
   * @param args Application arguments.
   * @param executionMode Application execution mode.
   * @param provider Application provider.
   * @return Application.
   */
  public static Jooby createApp(@Nonnull String[] args, @Nonnull ExecutionMode executionMode,
      @Nonnull Supplier<Jooby> provider) {

    configurePackage(provider);

    /** Dump command line as system properties. */
    parseArguments(args).forEach(System::setProperty);

    /** Fin application.env: */
    LogConfigurer.configure(new EnvironmentOptions().getActiveNames());

    Jooby app = provider.get();
    if (app.mode == null) {
      app.mode = executionMode;
    }
    return app;
  }

  private static void configurePackage(@Nonnull Object provider) {
    configurePackage(provider.getClass());
  }

  private static void configurePackage(@Nonnull Class providerClass) {
    if (!providerClass.getName().contains("KoobyKt")) {
      System.setProperty(DEF_PCKG,
          System.getProperty(DEF_PCKG, providerClass.getPackage().getName()));
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
        conf.putIfAbsent("application.env", arg);
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
      throw Throwing.sneakyThrow(x);
    }
  }

  private void fireStarted() {
    if (readyCallbacks != null) {
      fire(readyCallbacks);
      readyCallbacks = null;
    }
  }

  private void fire(List<Throwing.Runnable> tasks) {
    Iterator<Throwing.Runnable> iterator = tasks.iterator();
    while (iterator.hasNext()) {
      Throwing.Runnable task = iterator.next();
      task.run();
      iterator.remove();
    }
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
    configurePackage(consumer);
    return () -> {
      Jooby app = new Jooby();
      consumer.accept(app);
      return app;
    };
  }

  private static Supplier<Jooby> reflectionProvider(
      @Nonnull Class<? extends Jooby> applicationType) {
    return () ->
        (Jooby) Stream.of(applicationType.getDeclaredConstructors())
            .filter(it -> it.getParameterCount() == 0)
            .findFirst()
            .map(Throwing.throwingFunction(c -> c.newInstance()))
            .orElseThrow(() -> new IllegalArgumentException(
                "Default constructor for: " + applicationType.getName()));
  }
}
