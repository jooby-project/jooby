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

import io.jooby.internal.RegistryImpl;
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
import java.util.NoSuchElementException;
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

public class Jooby implements Router, Registry {

  static final String DEF_PCKG = "___def_package__";

  private RouterImpl router;

  private ExecutionMode mode;

  private Path tmpdir;

  private List<Throwing.Runnable> startCallbacks;

  private List<Throwing.Runnable> readyCallbacks;

  private LinkedList<AutoCloseable> stopCallbacks;

  private Env env;

  private Registry registry;

  private ServerOptions serverOptions;

  public Jooby() {
    router = new RouterImpl(getClass().getClassLoader());
  }

  public @Nullable ServerOptions getServerOptions() {
    return serverOptions;
  }

  @Nonnull @Override public RouterOptions getRouterOptions() {
    return router.getRouterOptions();
  }

  @Nonnull @Override public Jooby setRouterOptions(@Nonnull RouterOptions options) {
    router.setRouterOptions(options);
    return this;
  }

  public @Nonnull Jooby setServerOptions(@Nonnull ServerOptions serverOptions) {
    this.serverOptions = serverOptions;
    return this;
  }

  public @Nonnull Env getEnvironment() {
    if (env == null) {
      env = Env.defaultEnvironment(getClass().getClassLoader());
    }
    return env;
  }

  public @Nonnull Jooby setEnvironment(@Nonnull Env environment) {
    this.env = environment;
    return this;
  }

  public @Nonnull Jooby onStart(@Nonnull Throwing.Runnable task) {
    if (startCallbacks == null) {
      startCallbacks = new ArrayList<>();
    }
    startCallbacks.add(task);
    return this;
  }

  public @Nonnull Jooby onStarted(@Nonnull Throwing.Runnable task) {
    if (readyCallbacks == null) {
      readyCallbacks = new ArrayList<>();
    }
    readyCallbacks.add(task);
    return this;
  }

  public @Nonnull Jooby onStop(@Nonnull AutoCloseable task) {
    if (stopCallbacks == null) {
      stopCallbacks = new LinkedList<>();
    }
    stopCallbacks.addFirst(task);
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

  @Nonnull public Jooby install(@Nonnull Extension extension) {
    try {
      extension.install(this);
      return this;
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public Jooby dispatch(@Nonnull Runnable action) {
    router.dispatch(action);
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

  /** Error handler: */
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

  /** Log: */
  public @Nonnull Logger getLog() {
    return LoggerFactory.getLogger(getClass());
  }

  @Nonnull @Override public Jooby responseHandler(ResponseHandler handler) {
    router.responseHandler(handler);
    return this;
  }

  @Nonnull @Override public ErrorHandler getErrorHandler() {
    return router.getErrorHandler();
  }

  public @Nonnull Path getTmpdir() {
    return tmpdir;
  }

  public @Nonnull Jooby setTmpdir(@Nonnull Path tmpdir) {
    this.tmpdir = tmpdir;
    return this;
  }

  public @Nonnull ExecutionMode getExecutionMode() {
    return mode == null ? ExecutionMode.DEFAULT : mode;
  }

  public @Nonnull Jooby setExecutionMode(@Nonnull ExecutionMode mode) {
    this.mode = mode;
    return this;
  }

  @Nonnull @Override public AttributeMap getAttributes() {
    return router.getAttributes();
  }

  public @Nonnull <T> T require(@Nonnull Class<T> type, @Nonnull String name) {
    return require(new AttributeKey<>(type, name));
  }

  public @Nonnull <T> T require(@Nonnull Class<T> type) {
    return require(new AttributeKey<>(type));
  }

  public @Nonnull Jooby registry(@Nonnull Object registry) {
    this.registry = RegistryImpl.wrap(getClass().getClassLoader(), registry);
    return this;
  }

  private <T> T require(AttributeKey<T> key) {
    AttributeMap attributes = getAttributes();
    if (attributes.contains(key)) {
      return attributes.get(key);
    }
    if (registry != null) {
      String name = key.getName();
      if (name == null) {
        return registry.require(key.getType());
      }
      return registry.require(key.getType(), name);
    }
    throw new NoSuchElementException(key.toString());
  }

  public @Nullable String getBasePackage() {
    String classname = getClass().getName();
    if (classname.equals("io.jooby.Jooby") || classname.equals("io.jooby.Kooby")) {
      return System.getProperty(DEF_PCKG);
    }
    return getClass().getPackage().getName();
  }

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

  public @Nonnull Jooby start(@Nonnull Server server) {
    Env env = getEnvironment();
    if (tmpdir == null) {
      tmpdir = Paths.get(env.getConfig().getString("application.tmpdir")).toAbsolutePath();
    }

    /** Start router: */
    ensureTmpdir(tmpdir);

    if (mode == null) {
      mode = ExecutionMode.DEFAULT;
    }
    router.start(this);

    fireStart();

    return this;
  }

  public @Nonnull Jooby ready(@Nonnull Server server) {
    Logger log = getLog();

    fireStarted();

    log.info("{} started with:", getClass().getSimpleName());

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

  public @Nonnull Jooby stop() {
    if (router != null) {
      router.destroy();
      router = null;
    }

    fireStop();
    return this;
  }

  @Override public String toString() {
    return router.toString();
  }

  public static void runApp(String[] args, @Nonnull Class<? extends Jooby> applicationType) {
    runApp(args, ExecutionMode.DEFAULT, applicationType);
  }

  public static void runApp(String[] args, ExecutionMode executionMode,
      @Nonnull Class<? extends Jooby> applicationType) {
    configurePackage(applicationType);
    runApp(args, executionMode, () ->
        (Jooby) Stream.of(applicationType.getDeclaredConstructors())
            .filter(it -> it.getParameterCount() == 0)
            .findFirst()
            .map(Throwing.throwingFunction(c -> c.newInstance()))
            .orElseThrow(() -> new IllegalArgumentException(
                "Default constructor for: " + applicationType.getName()))
    );
  }

  public static void runApp(String[] args, @Nonnull Supplier<Jooby> provider) {
    runApp(args, ExecutionMode.DEFAULT, provider);
  }

  public static void runApp(String[] args, @Nonnull Consumer<Jooby> consumer) {
    runApp(args, ExecutionMode.DEFAULT, toProvider(consumer));
  }

  public static void runApp(@Nonnull String[] args, @Nonnull ExecutionMode executionMode,
      @Nonnull Consumer<Jooby> consumer) {
    runApp(args, executionMode, toProvider(consumer));
  }

  public static void runApp(@Nonnull String[] args, @Nonnull ExecutionMode executionMode,
      @Nonnull Supplier<Jooby> provider) {

    configurePackage(provider);

    /** Dump command line as system properties. */
    parseArguments(args).forEach(System::setProperty);

    /** Fin application.env: */
    String env = System.getProperty(Env.KEY, System.getenv().getOrDefault(Env.KEY, "dev"))
        .toLowerCase()
        .split(",")[0];

    logback(env.trim().toLowerCase());

    Jooby app = provider.get();
    if (app.mode == null) {
      app.mode = executionMode;
    }
    Server server = app.start();
    server.join();
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

  public static void logback(@Nonnull String env) {
    String logfile = System
        .getProperty("logback.configurationFile", System.getenv().get("logback.configurationFile"));
    if (logfile != null) {
      System.setProperty("logback.configurationFile", logfile);
    } else {
      Path userdir = Paths.get(System.getProperty("user.dir"));
      Path conf = userdir.resolve("conf");
      String logbackenv = "logback." + env + ".xml";
      String fallback = "logback.xml";
      Stream.of(
          /** Env specific inside conf or userdir: */
          conf.resolve(logbackenv), userdir.resolve(logbackenv),
          /** Fallback inside conf or userdir: */
          conf.resolve(fallback), userdir.resolve(fallback)
      ).filter(Files::exists)
          .findFirst()
          .map(Path::toAbsolutePath)
          .ifPresent(
              logback -> System.setProperty("logback.configurationFile", logback.toString()));
    }
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

  private void fireStart() {
    if (startCallbacks != null) {
      fire(startCallbacks);
      startCallbacks = null;
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

  private static Supplier<Jooby> toProvider(Consumer<Jooby> consumer) {
    configurePackage(consumer);
    return () -> {
      Jooby app = new Jooby();
      consumer.accept(app);
      return app;
    };
  }

}
