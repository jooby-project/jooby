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

import io.jooby.internal.RouteAnalyzer;
import io.jooby.internal.RouterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

public class Jooby implements Router {

  private RouterImpl router;

  private ExecutionMode mode;

  private Consumer<Server> serverConfigurer;

  private Path tmpdir;

  private List<Throwing.Runnable> startCallbacks;

  private List<Throwing.Runnable> readyCallbacks;

  private LinkedList<Throwing.Runnable> stopCallbacks;

  private Map<Object, Object> services = new HashMap<>();

  /**
   * Not ideal but useful. We want to have access to environment properties from instance
   * initializer. So external method before creating a new Jooby instance does a call to
   * {@link Jooby#setEnv(Env)} this makes available the env instance in any other Jooby instance
   * created.
   */
  private static ThreadLocal<Env> ENV = new ThreadLocal<>();

  protected Env environment;

  public Jooby() {
    router = new RouterImpl(new RouteAnalyzer(getClass().getClassLoader(), false));
    environment = ENV.get();
    if (environment == null) {
      // TODO: fallback for now, but probably better if we throws a specific error
      environment = Env.defaultEnvironment();
    }
    tmpdir = Paths.get(environment.get("application.tmpdir").value()).toAbsolutePath();

  }

  public Env environment() {
    return environment;
  }

  public Jooby environment(@Nonnull Env environment) {
    this.environment = environment;
    if (tmpdir == null) {
      tmpdir = Paths.get(environment.get("application.tmpdir").value()).toAbsolutePath();
    }
    return this;
  }

  @Nonnull @Override public Jooby caseSensitive(boolean caseSensitive) {
    this.router.caseSensitive(caseSensitive);
    return this;
  }

  @Nonnull @Override public Jooby ignoreTrailingSlash(boolean ignoreTrailingSlash) {
    this.router.ignoreTrailingSlash(ignoreTrailingSlash);
    return this;
  }

  public Jooby onStart(@Nonnull Throwing.Runnable task) {
    if (startCallbacks == null) {
      startCallbacks = new ArrayList<>();
    }
    startCallbacks.add(task);
    return this;
  }

  public Jooby onStarted(@Nonnull Throwing.Runnable task) {
    if (readyCallbacks == null) {
      readyCallbacks = new ArrayList<>();
    }
    readyCallbacks.add(task);
    return this;
  }

  public Jooby onStop(@Nonnull Throwing.Runnable task) {
    if (stopCallbacks == null) {
      stopCallbacks = new LinkedList<>();
    }
    stopCallbacks.addFirst(task);
    return this;
  }

  @Nonnull @Override public Jooby contextPath(@Nonnull String basePath) {
    router.contextPath(basePath);
    return this;
  }

  @Nonnull @Override public String contextPath() {
    return router.contextPath();
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

  @Nonnull @Override public List<Route> routes() {
    return router.routes();
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
    extension.install(this);
    return this;
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

  @Nonnull @Override public Executor worker() {
    return router.worker();
  }

  @Nonnull @Override public Jooby worker(Executor worker) {
    this.router.worker(worker);
    if (worker instanceof ExecutorService) {
      onStop(((ExecutorService) worker)::shutdown);
    }
    return this;
  }

  @Nonnull @Override public Jooby defaultWorker(@Nonnull Executor worker) {
    this.router.defaultWorker(worker);
    return this;
  }

  /** Log: */
  public Logger log() {
    return LoggerFactory.getLogger(getClass());
  }

  @Nonnull @Override public ErrorHandler errorHandler() {
    return router.errorHandler();
  }

  public Path tmpdir() {
    return tmpdir;
  }

  public Jooby tmpdir(@Nonnull Path tmpdir) {
    this.tmpdir = tmpdir;
    return this;
  }

  public ExecutionMode mode() {
    return mode;
  }

  public Jooby mode(ExecutionMode mode) {
    this.mode = mode;
    return this;
  }

  public <T> T require(Class<T> type) {
    return findService(type, type.getName());
  }

  public <T> T require(Class<T> type, String name) {
    return findService(type, type.getName() + "." + name);
  }

  private <T> T findService(Class<T> type, String key) {
    Object service = services.get(key);
    if (service == null) {
      throw new IllegalStateException("Service not found: " + type);
    }
    return type.cast(service);
  }

  public <T> Jooby addService(@Nonnull Class<T> type, @Nonnull T service) {
    putService(type, null, service);
    return this;
  }

  public <T> Jooby addService(@Nonnull T service) {
    putService(service.getClass(), null, service);
    return this;
  }

  public <T> Jooby addService(@Nonnull String name, @Nonnull T service) {
    putService(service.getClass(), name, service);
    return this;
  }

  public <T> Jooby addService(@Nonnull Class<T> type, @Nonnull String name, @Nonnull T service) {
    putService(type, name, service);
    return this;
  }

  private void putService(@Nonnull Class type, String name, @Nonnull Object service) {
    String defkey = type.getName();
    String key = type.getName();
    if (name != null) {
      key += "." + name;
    }
    services.put(key, service);
    services.putIfAbsent(defkey, service);
  }

  /** Boot: */
  public Server start() {
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
      log().warn("Multiple servers found {}. Using: {}", names, names.get(0));
    }
    Server server = servers.get(0);
    if (serverConfigurer != null) {
      serverConfigurer.accept(server);
    }
    return server.start(this);
  }

  public Jooby start(Server server) {
    /** Start router: */
    ensureTmpdir(tmpdir);
    Logger log = log();
    log.debug("environment:\n{}", environment);

    if (mode == null) {
      mode = ExecutionMode.DEFAULT;
    }
    router.start(this);

    fireStart();

    return this;
  }

  public Jooby ready(Server server) {
    Logger log = log();

    fireStarted();

    log.info("{} started with:", getClass().getSimpleName());

    log.info("    PID: {}", System.getProperty("PID"));
    log.info("    port: {}", server.port());
    log.info("    server: {}", server.getClass().getSimpleName().toLowerCase());
    log.info("    env: {}", environment.name());
    log.info("    thread mode: {}", mode.name().toLowerCase());
    log.info("    user: {}", System.getProperty("user.name"));
    log.info("    app dir: {}", System.getProperty("user.dir"));
    log.info("    tmp dir: {}", tmpdir);

    log.info("routes: \n\n{}\n\nlistening on:\n  http://localhost:{}{}\n", router, server.port(),
        router.contextPath());
    return this;
  }

  public Jooby stop() {
    if (router != null) {
      router.destroy();
      router = null;
    }

    fireStop();
    return this;
  }

  public Jooby configureServer(Consumer<Server> configurer) {
    this.serverConfigurer = configurer;
    return this;
  }

  @Override public String toString() {
    return router.toString();
  }

  public static void setEnv(Env environment) {
    ENV.set(environment);
  }

  public static void run(Supplier<Jooby> provider, String... args) {
    run(provider, ExecutionMode.DEFAULT, args);
  }

  public static void run(Supplier<Jooby> provider, ExecutionMode mode, String... args) {
    Server server;
    try {
      Env environment = Env.defaultEnvironment(args);
      setEnv(environment);

      logback(environment);

      Jooby app = provider.get();
      if (app.mode == null) {
        app.mode = mode;
      }
      server = app.start();
    } finally {
      // clear env
      setEnv(null);
    }
    server.join();
  }

  public static void logback(Env env) {
    String setfile = env.get("logback.configurationFile").value((String) null);
    if (setfile != null) {
      System.setProperty("logback.configurationFile", setfile);
      return;
    }
    String name = env.name();
    Path userdir = Paths.get(System.getProperty("user.dir"));
    Path conf = userdir.resolve("conf");
    String logbackenv = "logback." + name + ".xml";
    String fallback = "logback.xml";
    Stream.of(
        /** Env specific inside conf or userdir: */
        conf.resolve(logbackenv), userdir.resolve(logbackenv),
        /** Fallback inside conf or userdir: */
        conf.resolve(fallback), userdir.resolve(fallback)
    ).filter(Files::exists)
        .findFirst()
        .map(Path::toAbsolutePath)
        .ifPresent(logback -> System.setProperty("logback.configurationFile", logback.toString()));
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
      List<Throwing.Runnable> tasks = stopCallbacks;
      stopCallbacks = null;
      Iterator<Throwing.Runnable> iterator = tasks.iterator();
      while (iterator.hasNext()) {
        Throwing.Runnable task = iterator.next();
        try {
          task.run();
        } catch (Exception x) {
          log().info("exception found while executing onStop:", x);
        }
        iterator.remove();
      }
    }
  }
}
